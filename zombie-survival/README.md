# Nagel Zombie Survival

Servidor separado de apocalipse zumbi para Java e Bedrock/PS5. Ele nao usa o
mundo nem os dados do Nagel Craftlandia.

## Portas

- Java: TCP `25566`.
- Bedrock/PS5: UDP `19133`.
- Dados locais: volume Docker `zombie-data`.
- Dados no Coolify: `/data/minecraft-nagel/zombie-data`.

## Armas

| Arma | Controle | Dano base | Pente | Alcance |
| --- | --- | ---: | ---: | ---: |
| Pistola 9mm | ataque/R2 | 3 coracoes | 12 | 38 blocos |
| Escopeta | ataque/R2 | ate 12 coracoes | 6 | 24 blocos |
| Rifle de assalto | ataque/R2 | 6 coracoes | 30 | 65 blocos |
| Rifle de precisao | L2 para mirar e disparar ao sair da mira | 15 coracoes | 5 | 130 blocos |

Use o botao secundario ou o controle `Soltar item` para recarregar; a arma nao
e jogada no chao. Na sniper, use `agachar + botao secundario`, pois o secundario
sozinho ativa a mira. No Java, `Soltar item` pode ser associado a tecla `R`.
`/recarregar` faz a mesma coisa. A recarga tambem inicia automaticamente quando
o pente termina. Cada arma possui municao propria e todas as receitas aparecem
no livro da bancada. Operadores podem usar `/kitarmas` para receber o arsenal
de teste.

## Sobrevivencia

- `/nutricao`: carboidratos, proteinas, gorduras, vitaminas e hidratacao.
- `/estacao`: estacao, dia do mundo e temperatura atual.
- `/sethome` e `/home`: casa pessoal, sem depender de plugins externos.
- Temperatura considera estacao, bioma, clima, horario, altitude, agua e calor
  proximo.
- Monstros naturais comuns sao substituidos por infectados; aranhas, creepers
  e esqueletos nao ocupam o mapa.
- Cambaleantes, errantes, cacadores, corredores, brutamontes e Berserkers possuem
  velocidades, vida e percepcao diferentes.
- 78% dos infectados sao lentos; cacadores, corredores, brutamontes e alfas
  formam os 22% restantes.
- Zumbis nao queimam durante o dia e emitem gemidos com tons diferentes.
- Tres corpos redesenhados, com pele continua, feridas localizadas e seis
  conjuntos de roupa geram varias aparencias sem o efeito pontilhado.
- O Berserker possui escala 1,45x, 40 coracoes, velocidade baixa, ataque forte
  e alta resistencia a recuo.
- Infectados usam tres gemidos graves proprios. O Berserker possui um rugido
  exclusivo combinado com sons nativos, garantindo impacto tambem no PS5.
- A pistola possui recuo leve, o rifle recuo medio, a sniper recuo forte e a
  escopeta empurra proporcionalmente ao numero de chumbos que acertam.
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

`./build.sh` compila o plugin, gera as texturas, os modelos 3D das armas e cria
os dois pacotes em `dist/`.
