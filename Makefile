.PHONY: start stop restart logs status backup backups restore allowlist disable-allowlist craft-start craft-start-resilient craft-stop craft-restart craft-logs craft-status craft-cmd craft-op craft-setup-perms craft-whitelist craft-disable-whitelist craft-backup craft-backup-cloud craft-cloud-latest craft-restore-cloud-latest craft-backups craft-restore vps-craft-backup-cloud vps-craft-restore-cloud-latest install-backup-cron remove-backup-cron install-craft-backup-cron remove-craft-backup-cron install-craft-cloud-backup-cron remove-craft-cloud-backup-cron cron update cmd

start:
	docker compose up -d

stop:
	docker compose stop

restart:
	docker compose restart bedrock

logs:
	docker compose logs -f bedrock

status:
	docker compose ps

backup:
	./scripts/backup.sh

backups:
	find backups -name "bedrock-*.tar.gz" -type f 2>/dev/null | sort

restore:
	@test -n "$(FILE)" || (echo 'Uso: make restore FILE="backups/bedrock-YYYYMMDD-HHMMSS.tar.gz"'; exit 1)
	./scripts/restore.sh "$(FILE)"

allowlist:
	@test -n "$(USERS)" || (echo 'Uso: make allowlist USERS="Gamertag1,Gamertag2"'; exit 1)
	./scripts/set-allowlist.sh "$(USERS)"

disable-allowlist:
	./scripts/disable-allowlist.sh

craft-start:
	docker compose stop bedrock
	docker compose --profile craftlandia up -d craftlandia

craft-start-resilient:
	./scripts/start-craftlandia-resilient.sh

craft-stop:
	docker compose --profile craftlandia stop craftlandia

craft-restart:
	docker compose --profile craftlandia restart craftlandia

craft-logs:
	docker compose --profile craftlandia logs -f craftlandia

craft-status:
	docker compose --profile craftlandia ps craftlandia

craft-cmd:
	@test -n "$(CMD)" || (echo 'Uso: make craft-cmd CMD="say oi"'; exit 1)
	docker compose --profile craftlandia exec -T craftlandia rcon-cli "$(CMD)"

craft-op:
	@test -n "$(USER)" || (echo 'Uso: make craft-op USER="Gamertag"'; exit 1)
	docker compose --profile craftlandia exec -T craftlandia rcon-cli "op $(USER)"

craft-setup-perms:
	./scripts/setup-craftlandia-permissions.sh

craft-whitelist:
	@test -n "$(USERS)" || (echo 'Uso: make craft-whitelist USERS=".Gamertag1,.Gamertag2"'; exit 1)
	./scripts/set-craftlandia-whitelist.sh "$(USERS)"

craft-disable-whitelist:
	./scripts/disable-craftlandia-whitelist.sh

craft-backup:
	./scripts/backup-craftlandia.sh

craft-backup-cloud:
	./scripts/backup-craftlandia-cloud.sh

craft-cloud-latest:
	./scripts/download-latest-craftlandia-backup.sh --dry-run

craft-restore-cloud-latest:
	./scripts/restore-latest-craftlandia-cloud.sh

craft-backups:
	find craftlandia-backups -name "craftlandia-*.tar.gz" -type f 2>/dev/null | sort

craft-restore:
	@test -n "$(FILE)" || (echo 'Uso: make craft-restore FILE="craftlandia-backups/craftlandia-YYYYMMDD-HHMMSS.tar.gz"'; exit 1)
	./scripts/restore-craftlandia.sh "$(FILE)"

vps-craft-backup-cloud:
	./scripts/vps-backup-craftlandia-cloud.sh

vps-craft-restore-cloud-latest:
	./scripts/vps-restore-latest-craftlandia-cloud.sh

install-backup-cron:
	./scripts/install-backup-cron.sh

remove-backup-cron:
	./scripts/remove-backup-cron.sh

install-craft-backup-cron:
	./scripts/install-craftlandia-backup-cron.sh

remove-craft-backup-cron:
	./scripts/remove-craftlandia-backup-cron.sh

install-craft-cloud-backup-cron:
	./scripts/install-craftlandia-cloud-backup-cron.sh

remove-craft-cloud-backup-cron:
	./scripts/remove-craftlandia-cloud-backup-cron.sh

cron:
	crontab -l

update:
	docker compose pull bedrock
	docker compose up -d

cmd:
	@test -n "$(CMD)" || (echo 'Uso: make cmd CMD="say oi"'; exit 1)
	docker exec nagel-bedrock send-command $(CMD)
