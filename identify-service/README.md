# Identify service

Requer Java 25 e PostgreSQL (a configuração local usa o banco `identify_service`).

## Chaves JWT

Forneça um par RSA correspondente. Para desenvolvimento, gere as chaves em uma pasta fora do repositório:

```sh
mkdir -p /tmp/identify-service-keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out /tmp/identify-service-keys/private.pem
openssl pkey -in /tmp/identify-service-keys/private.pem -pubout -out /tmp/identify-service-keys/public.pem
chmod 600 /tmp/identify-service-keys/private.pem
export JWT_PRIVATE_KEY=file:/tmp/identify-service-keys/private.pem
export JWT_PUBLIC_KEY=file:/tmp/identify-service-keys/public.pem
./mvnw spring-boot:run
```

Não versione a chave privada. Configure `JWT_PRIVATE_KEY` e `JWT_PUBLIC_KEY` com localizações `file:` ou `classpath:`. Os padrões são `classpath:keys/private.pem` e `classpath:keys/public.pem`; a chave privada não é distribuída. `JWT_EXPIRATION_SECONDS` controla a validade (padrão: 3600 segundos, deve ser positivo).

## Autenticação

As rotas de autenticação usam o prefixo `/v1/auth`.

`POST /v1/auth/register` recebe:

```json
{"cpf":"12345678901","fullName":"Lucas","email":"lucas@example.com","password":"password123"}
```

Retorna HTTP 201. CPF deve conter 11 dígitos, e a senha deve ter pelo menos 8 caracteres. Contas são criadas como `CUSTOMER` e `ACTIVE`; CPF/e-mail duplicados retornam 409.

`POST /v1/auth/login` recebe:

```json
{"cpf":"12345678901","password":"password123"}
```

Retorna HTTP 200. As duas rotas respondem:

```json
{"accessToken":"<JWT>","tokenType":"Bearer","expiresIn":3600}
```

Envie `Authorization: Bearer <JWT>` nas demais rotas, por exemplo `GET /v1/users/me`. Credenciais incorretas ou conta bloqueada retornam 401 com mensagem genérica; dados inválidos retornam 400. Bloquear uma conta impede novos logins; tokens já emitidos continuam válidos até expirar. Não há refresh token ou logout nesta versão.

Swagger: `/swagger-ui/index.html`.

## Testes

Execute a suíte completa a partir da raiz do repositório:

```sh
./identify-service/scripts/test-auth.sh
```

O script aguarda o container `pg-test-identify-service` saudável na porta `5433` e roda `./mvnw test`. Os testes de autenticação usam HTTP real, Flyway e um par RSA temporário. Eles limpam os usuários do banco de teste antes de cada cenário. Nunca configure os testes para a instância principal na porta `5432`; o container de teste permanece rodando ao final.
