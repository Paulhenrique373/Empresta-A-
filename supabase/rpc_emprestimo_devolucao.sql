-- Proposta para revisão. Não executar sem validar nomes/constraints do banco real.
-- Pressupõe status 'Disponível', 'Em uso', 'Manutenção' e empréstimo 'Ativo'/'Devolvido'.
create or replace function public.emprestar_equipamento(p_id_equipamento bigint)
returns bigint language plpgsql security invoker set search_path = '' as $$
declare v_id bigint; v_ativo boolean;
begin
  select u.ativo into v_ativo from public.usuarios u where u.id_usuario = auth.uid();
  if coalesce(v_ativo, false) is not true then raise exception 'Conta inativa ou sem cadastro'; end if;
  if not exists (select 1 from public.usuario_perfil up join public.perfis p using (id_perfil)
    where up.id_usuario = auth.uid() and p.nome_perfil in ('Professor FGB','Professor Técnico','PAEET','Gestão')) then
    raise exception 'Perfil sem permissão para empréstimo';
  end if;
  update public.equipamentos set status = 'Em uso'
    where id_equipamento = p_id_equipamento and status = 'Disponível'
      and not exists (select 1 from public.emprestimos e where e.id_equipamento = p_id_equipamento and e.status = 'Ativo');
  if not found then raise exception 'Equipamento indisponível'; end if;
  insert into public.emprestimos(id_usuario,id_equipamento,data_retirada,status)
    values(auth.uid(),p_id_equipamento,now(),'Ativo') returning id_emprestimo into v_id;
  insert into public.auditoria(id_usuario,acao,tabela_afetada,registro_id,data_acao,detalhes)
    values(auth.uid(),'EMPRESTIMO','emprestimos',v_id,now(),jsonb_build_object('id_equipamento',p_id_equipamento));
  return v_id;
end $$;

create or replace function public.devolver_equipamento(p_id_emprestimo bigint)
returns void language plpgsql security invoker set search_path = '' as $$
declare v_equipamento bigint;
begin
  update public.emprestimos set status='Devolvido', data_devolucao=now()
    where id_emprestimo=p_id_emprestimo and status='Ativo'
      and (id_usuario=auth.uid() or exists(select 1 from public.usuario_perfil up join public.perfis p using(id_perfil)
        where up.id_usuario=auth.uid() and p.nome_perfil='Gestão'))
    returning id_equipamento into v_equipamento;
  if not found then raise exception 'Empréstimo ativo não encontrado ou sem permissão'; end if;
  update public.equipamentos set status='Disponível'
    where id_equipamento=v_equipamento and status='Em uso'
      and not exists(select 1 from public.defeitos d where d.id_equipamento=v_equipamento and d.status='Em manutenção');
  insert into public.auditoria(id_usuario,acao,tabela_afetada,registro_id,data_acao,detalhes)
    values(auth.uid(),'DEVOLUCAO','emprestimos',p_id_emprestimo,now(),jsonb_build_object('id_equipamento',v_equipamento));
end $$;

-- Exige policies RLS que autorizem as linhas acima; SECURITY INVOKER mantém RLS ativa.
