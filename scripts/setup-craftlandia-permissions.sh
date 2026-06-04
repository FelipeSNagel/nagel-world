#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

run_cmd() {
  docker compose --profile craftlandia exec -T craftlandia rcon-cli "$1"
}

echo "Configuring default Craftlandia permissions..."

run_cmd "lp group default permission set essentials.spawn true"
run_cmd "lp group default permission set essentials.sethome true"
run_cmd "lp group default permission set essentials.home true"
run_cmd "lp group default permission set essentials.delhome true"
run_cmd "lp group default permission set essentials.tpa true"
run_cmd "lp group default permission set essentials.tpaccept true"
run_cmd "lp group default permission set essentials.tpdeny true"
run_cmd "lp group default permission set essentials.balance true"
run_cmd "lp group default permission set essentials.pay true"
run_cmd "lp group default permission set essentials.balancetop true"
run_cmd "lp group default permission set essentials.sell true"
run_cmd "lp group default permission set essentials.sell.hand true"
run_cmd "lp group default permission set essentials.sell.all true"
run_cmd "lp group default permission set essentials.sell.inventory true"
run_cmd "lp group default permission set essentials.worth true"
run_cmd "lp group default permission set essentials.msg true"
run_cmd "lp group default permission set essentials.warp true"
run_cmd "lp group default permission set essentials.warps.* true"
run_cmd "lp group default permission set essentials.warps.mercado true"
run_cmd "lp group default permission set griefprevention.createclaims true"
run_cmd "lp group default permission set griefprevention.claims true"
run_cmd "lp group default permission set quickshop.use true"
run_cmd "lp group default permission set quickshop.create.sell true"
run_cmd "lp group default permission set quickshop.create.buy true"
run_cmd "lp group default permission set quickshop.create.changeprice true"
run_cmd "lp group default permission set quickshop.create.changeamount true"
run_cmd "lp group default permission set quickshop.create.double true"
run_cmd "lp group default permission set quickshop.create.stacks true"
run_cmd "lp group default permission set quickshop.find true"

echo "Default permissions configured."
