# Regras do identify-service

- Ao implementar, siga as instruções e use as skills, ferramentas, scripts e referências aplicáveis de `.cursor/` e `.claude/` na raiz do repositório; leia o `SKILL.md` antes de usar cada skill.
- Execute toda a suíte da raiz com `./identify-service/scripts/test-auth.sh`.
- O script sobe e aguarda `pg-test-identify-service` e executa `./mvnw test`.
- A autenticação usa HTTP real e o perfil `auth-test` com PostgreSQL e Flyway.
- O banco de teste é `identify_service`, usuário/senha `postgres`, porta `5433`.
- Nunca use nem limpe o banco principal na porta `5432`.
- A limpeza de usuários ocorre explicitamente antes de cada cenário HTTP.
- Se houver falha, corrija a causa e repita o comando completo.
- Mantenha o container de teste rodando após validar.
- As chaves RSA dos testes são temporárias e compartilhadas pelo suporte de teste.
- O fluxo de autenticação cobre cadastro, login, `/v1/users/me` e falhas de JWT.
- Preserve os demais testes e suas configurações de banco existentes.
