#!/usr/bin/env python3
import os
import re

root = 'game-server/src/main/java/net/dodian/uber/game/netty/listener/in'
forbidden_patterns = [
    'client.send(',
    'client.reset',
    'client.face',
    'client.decline',
    'client.bank',
    'client.trade',
    'client.stake',
    'client.fromBank(',
    'client.fromTrade(',
    'client.fromDuel(',
    'client.addItem(',
    'client.deleteItem(',
    'client.dropItem(',
    'client.pickUpItem(',
    'client.wear(',
    'client.remove(',
    'client.checkItemUpdate(',
    'client.showNPCChat(',
    'client.tradeReq(',
    'client.close',
    'client.open',
    'client.clearBankStyleView(',
    'client.setLook(',
    'client.setChatText',
    'client.setWindowFocused(',
    'client.invalidate',
    'PlayerActionCancellationService.cancel(',
    'DialogueService.closeBlockingDialogue(',
]
mutation_re = re.compile(r'\bclient\.[A-Za-z_][A-Za-z0-9_]*\s*(=|\+=|-=|\*=|/=)')

violations = []
for fname in sorted(os.listdir(root)):
    if not fname.endswith('.java'):
        continue
    path = os.path.join(root, fname)
    with open(path) as f:
        lines = f.readlines()
    for i, line in enumerate(lines):
        t = line.strip()
        if not t:
            continue
        if t.startswith('//') or t.startswith('/*') or t.startswith('*'):
            continue
        if t.startswith('package ') or t.startswith('import '):
            continue
        hits = [p for p in forbidden_patterns if p in t]
        if hits:
            violations.append(f'{fname}:{i+1}: {t[:100]}')
            continue
        if mutation_re.search(t):
            violations.append(f'{fname}:{i+1} [MUTATION]: {t[:100]}')

for v in violations:
    print(v)
print(f'\nTotal: {len(violations)} violations')

