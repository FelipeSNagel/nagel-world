# Local Bridge para PS5

Proxy RakNet local baseado no
[Phantom](https://github.com/jhead/phantom), projeto feito especificamente
para mostrar servidores Bedrock externos como jogos LAN em consoles.

O PS5 encontra o servidor da VPS na aba de amigos e o Mac encaminha os
pacotes durante toda a partida.

## Rodar

```bash
make install
make start
```

Nas proximas vezes, somente `make start` e necessario. Na primeira execucao,
o macOS pode perguntar se o Phantom pode receber conexoes de rede. Permita.

## Iniciar automaticamente no Mac

Para nao precisar abrir terminal nem aplicativo antes de jogar:

```bash
make service-install
```

O bridge passa a iniciar junto com a sua sessao do macOS e reinicia sozinho
se o processo cair.

Comandos de manutencao:

```bash
make service-status
make service-logs
make service-uninstall
```

O computador e o PS5 precisam estar na mesma rede. No Minecraft do PS5, abra
`Jogar`, entre na aba de amigos e selecione o servidor exibido na lista LAN.

Para mudar o servidor, copie o arquivo de configuracao e edite os valores:

```bash
cp .env.example .env
```

O Mac precisa permanecer ligado e o bridge precisa continuar rodando durante
toda a partida, pois ele encaminha os pacotes entre o PS5 e a VPS.

Nao rode outro servidor ou bridge Bedrock local ao mesmo tempo, pois o
Phantom precisa escutar a porta UDP `19132` para anunciar o jogo.

## Estrutura isolada

Tudo que o bridge utiliza fica nesta pasta:

- `scripts/`: inicializacao manual e servico automatico do macOS;
- `bin/`: binario local do Phantom, baixado na instalacao e ignorado pelo Git;
- `logs/`: logs do servico automatico, ignorados pelo Git;
- `.env.example`: valores de configuracao.

O instalador fixa a versao `v0.5.4`, que possui build nativa para Apple
Silicon. O binario nao e versionado neste repositorio.
