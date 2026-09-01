# Nagel Zombie Survival

Servidor separado de apocalipse zumbi para Java e Bedrock/PS5. Ele nao usa o
mundo nem os dados do Nagel Craftlandia.

## Portas

- Java: TCP `25566`.
- Bedrock/PS5: UDP `19133`.
- Dados locais: volume Docker `zombie-data`.
- Dados no Coolify: `/data/minecraft-nagel/zombie-data`.

## Armas

| Arma | Controle | Pente | Alcance |
| --- | --- | ---: | ---: |
| Pistola 9mm | usar/L2 | 12 | 38 blocos |
| Escopeta | usar/L2 | 6 | 24 blocos |
| Rifle de assalto | usar/L2 | 30 | 65 blocos |
| Rifle de precisao | segurar L2 para mirar e R2 para atirar | 5 | 130 blocos |

A recarga e automatica quando o pente termina. Cada arma possui municao
propria e todas as receitas aparecem no livro da bancada. Operadores podem usar
`/kitarmas` para receber o arsenal de teste.

## Sobrevivencia

- `/nutricao`: carboidratos, proteinas, gorduras, vitaminas e hidratacao.
- `/estacao`: estacao, dia do mundo e temperatura atual.
- `/sethome` e `/home`: casa pessoal, sem depender de plugins externos.
- Temperatura considera estacao, bioma, clima, horario, altitude, agua e calor
  proximo.
- Corredores, brutamontes e infectados aparecem entre zumbis normais.
- Tiros atraem zumbis.
- A cada dez dias surge uma horda durante a noite.
- Zumbis perseguindo jogadores racham e quebram blocos. Baus, portais,
  obsidiana, bedrock e os 12 blocos ao redor do spawn sao protegidos.

## Rodar localmente

Docker Desktop precisa estar aberto.

```sh
cd zombie-survival
cp .env.example .env
make start
make logs
```

Java conecta em `localhost:25566`. Para o PS5, a bridge deve apontar para o IP
local do Mac na porta UDP `19133`.

## Coolify

Crie uma nova aplicacao Docker Compose usando o mesmo repositorio:

- base directory: `/zombie-survival`;
- compose file: `/docker-compose.coolify.yml`;
- abra `25566/TCP` e `19133/UDP` no firewall da VPS;
- defina `ZOMBIE_RCON_PASSWORD` com uma senha forte;
- opcionalmente habilite a whitelist e informe todos os nomes em
  `ZOMBIE_WHITELIST`.

O pacote Bedrock fica dentro do container e e enviado pelo Geyser. O Java baixa
`dist/NagelZombieJava.zip` do repositorio publico. Sempre que o pacote for
reconstruido, atualize `ZOMBIE_RESOURCE_PACK_SHA1` com o conteudo de
`dist/NagelZombieJava.sha1`.

## Desenvolvimento

`./build.sh` compila o plugin, gera as texturas e cria os dois pacotes em
`dist/`. Os modelos atuais formam a prova de conceito; podem ser substituidos
por modelos 3D mais detalhados sem alterar armas, crafting ou mundo.
