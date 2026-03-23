# Magnum Fipe Monorepo

Este projeto é um sistema distribuído composto por duas APIs desenvolvidas em **Java 21** com o framework **Quarkus**, seguindo os princípios de **Clean Architecture**, **DDD**, **SOLID** e **Contract First (Swagger/OpenAPI)**.

O objetivo do sistema é realizar a carga de dados da tabela FIPE (marcas e modelos de veículos), processar essas informações de forma assíncrona via mensageria e disponibilizar endpoints para consulta e atualização.

## Arquitetura do Sistema

- **api-1 (REST API & Producer)**:
  - Expõe endpoints REST para disparar a carga inicial e consultar dados.
  - Envia marcas de veículos para uma fila no RabbitMQ.
  - Gerencia cache via Redis para consultas de veículos.
  - Protegida por autenticação JWT.

- **api-2 (Worker & Consumer)**:
  - Consome as marcas da fila do RabbitMQ.
  - Consulta a API pública da FIPE para buscar modelos de cada marca.
  - Persiste as informações no banco de dados PostgreSQL.

## Diagramas (Mermaid)

Para visualizar os diagramas, você pode utilizar o [Mermaid Live Editor](https://mermaid.live/) ou uma extensão compatível em sua IDE:
- `domain.mermaid`: Contém a modelagem das entidades de domínio e seus relacionamentos.
- `sequenceDiagram.mermaid`: Ilustra o fluxo de comunicação entre as APIs, RabbitMQ, Fipe API e o banco de dados.

## Autenticação

O sistema utiliza JWT para autenticação com validade de 30 minutos. 
Para acessar os endpoints protegidos, você deve primeiro obter um token:

1. Chame o endpoint `POST /auth/token` na **API-1** (o endpoint de carga inicial está na `api-1`).
2. O corpo da resposta será o token JWT em texto puro.
3. Utilize este token no cabeçalho `Authorization: Bearer <seu_token>` para as demais requisições.

Os endpoints protegidos em `api-1` (`/fipe/*`) exigem o papel `user`, incluído no token gerado pelo endpoint acima.

## Tecnologias Utilizadas

- **Java 21**
- **Quarkus 3.x**
- **PostgreSQL** (Banco de dados relacional)
- **RabbitMQ** (Mensageria/Fila)
- **Redis** (Cache)
- **Panache ORM** (Acesso a dados)
- **SmallRye Reactive Messaging** (Integração RabbitMQ)
- **MicroProfile Rest Client** (Consumo de APIs externas)
- **Docker & Docker Compose**

---

## Pré-requisitos

- **Java 21** instalado.
- **Maven 3.9+** instalado.
- **Docker** e **Docker Compose** instalados.

---

## Como Executar

### 1. Subir a Infraestrutura

Na raiz do projeto, execute o comando para iniciar apenas os serviços de suporte (Banco de dados, Broker de mensagens e Redis):

```bash
docker-compose -f docker-compose.dev.yml up -d
```

Isso iniciará:
- **PostgreSQL**: Porta 5432
- **RabbitMQ**: Portas 5672 (AMQP) e 15672 (Management Console)
- **Redis**: Porta 6379

Para encerrar a infraestrutura:
```bash
docker-compose -f docker-compose.dev.yml down
```

### 2. Compilar o Projeto

A partir da raiz do monorepo, compile todos os módulos:

```bash
mvn clean compile
```

### 3. Executar as APIs

Você pode executar as APIs de duas maneiras:

#### Opção A: Docker Compose Completo (Recomendado)

1. Realize o build do projeto Maven na raiz:
   ```bash
   mvn clean install -DskipTests
   ```

2. Suba todos os serviços e as APIs:
   ```bash
   docker-compose up --build
   ```

A **api-1** estará disponível em `http://localhost:8080/q/swagger-ui`.

#### Opção B: Execução Local (Desenvolvimento)

Você pode executar as APIs simultaneamente em terminais diferentes.

#### Executar api-1 (Porta 8081):
```bash
cd api-1
./mvnw quarkus:dev -Dquarkus.http.port=8081
```

#### Executar api-2 (Porta 8082):
```bash
cd api-2
./mvnw quarkus:dev -Dquarkus.http.port=8082
```

---

## Endpoints Principais (api-1)

A documentação completa (Swagger) pode ser acessada em: `http://localhost:8080/q/swagger-ui`

- `POST /fipe/load`: Aciona a carga inicial de marcas.
- `GET /fipe/brands`: Lista as marcas armazenadas no banco.
- `GET /fipe/vehicles/{brandCode}`: Busca veículos de uma marca (com cache Redis).
- `PUT /fipe/vehicles/{brandCode}/{vehicleCode}`: Atualiza modelo/observações de um veículo.

*Nota: Os endpoints da api-1 exigem autenticação via JWT conforme os requisitos.*

---

## Exemplos de Uso (cURL)

Abaixo estão exemplos de comandos `curl` para interagir com a API-1 (ajuste a porta se necessário).

### 1. Obter Token JWT
```bash
curl -X POST http://localhost:8080/auth/token
```

### 2. Acionar Carga Inicial (Protegido)
```bash
# Substitua <TOKEN> pelo valor retornado no passo 1
curl -X POST http://localhost:8080/fipe/load \
     -H "Authorization: Bearer <TOKEN>"
```

### 3. Listar Marcas (Protegido)
```bash
curl -X GET http://localhost:8080/fipe/brands \
     -H "Authorization: Bearer <TOKEN>"
```

### 4. Buscar Veículos por Marca (Protegido)
*Exemplo para a marca '21' (Fiat):*
```bash
curl -X GET http://localhost:8080/fipe/vehicles/21 \
     -H "Authorization: Bearer <TOKEN>"
```

### 5. Atualizar Veículo (Protegido)
*Exemplo para a marca '21' e código do veículo '620' (Uno):*
```bash
curl -X PUT http://localhost:8080/fipe/vehicles/21/620 \
     -H "Authorization: Bearer <TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{
           "modelo": "Uno Mille 1.0",
           "observacoes": "Veículo revisado com troca de óleo e filtros"
         }'
```

---

## Estrutura de Pastas (Clean Architecture)

Ambos os módulos seguem a estrutura:
- `domain`: Entidades ricas, regras de negócio e interfaces de repositórios/serviços.
- `application`: DTOs e casos de uso.
- `infrastructure`: Implementações técnicas (Persistência, Clientes HTTP, Mensageria).
- `presentation`: Entrypoints da aplicação (Recursos REST, Consumers de Fila).
