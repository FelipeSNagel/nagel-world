.PHONY: build start stop restart logs status cmd kit clean

build:
	./build.sh

start: build
	docker compose up -d --build

stop:
	docker compose stop

restart:
	docker compose restart zombie-server

logs:
	docker compose logs -f zombie-server

status:
	docker compose ps

cmd:
	@test -n "$(CMD)" || (echo 'Uso: make cmd CMD="say oi"'; exit 1)
	docker compose exec -T zombie-server rcon-cli "$(CMD)"

kit:
	@test -n "$(USER)" || (echo 'Uso: make kit USER="FelipeSNagel"'; exit 1)
	docker compose exec -T zombie-server rcon-cli "kitarmas $(USER)"

clean:
	rm -rf build

