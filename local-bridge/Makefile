.PHONY: install start check service-install service-uninstall service-status service-logs

install:
	./scripts/install.sh

start: install
	./scripts/run.sh

check:
	zsh -n scripts/*.sh
	test -x bin/phantom
	@echo "Local bridge pronto."

service-install:
	./scripts/install-service.sh

service-uninstall:
	./scripts/uninstall-service.sh

service-status:
	launchctl print gui/$$(id -u)/com.nagel.local-bridge

service-logs:
	tail -f logs/local-bridge.log logs/local-bridge.error.log
