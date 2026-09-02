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
| Submetralhadora | segurar ataque/R2 | 2 coracoes | 60 | 44 blocos |
| Escopeta | ataque/R2 | ate 12 coracoes | 6 | 24 blocos |
| Rifle de assalto | ataque/R2 | 6 coracoes | 30 | 65 blocos |
| Rifle de precisao | L2 para mirar e disparar ao sair da mira | 15 coracoes | 5 | 130 blocos |

Use o botao secundario ou o controle `Soltar item` para recarregar; a arma nao
e jogada no chao. Na sniper, use `agachar + botao secundario`, pois o secundario
sozinho ativa a mira. No Java, altere em Controles a tecla de `Soltar item` de
`Q` para `R`: o servidor recebe a acao, mas nao consegue identificar uma tecla
fisica arbitraria sem exigir um mod no cliente.
`/recarregar` faz a mesma coisa. A recarga tambem inicia automaticamente quando
o pente termina. Cada arma usa sua municao adequada e todas as receitas aparecem
no livro da bancada. Operadores podem usar `/kitarmas` para receber o arsenal
de teste.

A submetralhadora usa a mesma municao 9 mm da pistola. Segure o ataque para
manter a rajada automatica de 10 tiros por segundo.
Ataques corpo a corpo continuam com o dano normal do Minecraft. Quando um
cliente Bedrock envia apenas a animacao do golpe, o servidor aplica um golpe de
proximidade equivalente sem duplicar o dano nos clientes Java.

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
- A cada dez dias surge uma horda durante a noite, sorteada entre 30 e 100
  infectados no total e dividida aleatoriamente em uma a cinco fases. Cada fase
  recebe uma quantidade propria, com uma pausa curta entre elas, e libera ondas
  de cinco por segundo para manter a pressao sem congelar o servidor.
- Hordas com nascimento valido disparam uma sirene grave original de sete
  segundos. O titulo aparece depois do inicio do alerta; sons nativos em camada
  garantem o aviso tambem nos clientes de PlayStation.
- A noite comum aceita no maximo oito zumbis carregados por jogador. A horda
  especial amplia temporariamente esse limite pelo tamanho sorteado e bloqueia
  nascimentos naturais adicionais ate o amanhecer.
- Infectados da horda nao ficam vagando: recebem alcance de perseguicao maior,
  um pequeno aumento de velocidade e recalculam a cada segundo o jogador
  sobrevivente mais proximo como objetivo.
- Infectados surgem a pelo menos 36 blocos, somente ao ar livre, sobre piso
  solido e com luz de blocos igual a zero. Tetos, tochas, lanternas e outras
  fontes de luz impedem novos nascimentos dentro das construcoes.
- Cerca de 16% dos zumbis racham e quebram blocos; tipos fortes possuem maior
  chance e todo Berserker e quebrador. Baus, portais, obsidiana, bedrock e os
  12 blocos ao redor do spawn sao protegidos.
- Cerca de 12% dos zumbis constroem degraus de pedra quando o alvo esta acima.
  Os degraus desaparecem automaticamente depois de 90 segundos; Berserkers
  tambem possuem essa habilidade.

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
