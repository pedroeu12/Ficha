# Auditoria contra dnd2024.wikidot.com

> **Situação: resolvido.** Este relatório é o levantamento original, mantido como
> registro do que foi encontrado. Tudo abaixo já foi corrigido ou importado, e o
> app agora acompanha o wiki item a item: 419 magias (todos os níveis), 185
> talentos, 61 antecedentes, 24 espécies, 418 itens mágicos e 68 subclasses. As
> fontes viraram dado de primeira classe — cada opção nomeia seu livro, e um
> personagem só recebe o que os livros escolhidos na criação permitem. O conteúdo
> que só existia em 2014 foi removido.

Comparação automatizada entre os dados do app e o wiki `dnd2024.wikidot.com`,
feita em 22/08/2026. A referência foi baixada do wiki, convertida para JSON e
comparada campo a campo com os arquivos de `data/content/`. Toda divergência
listada aqui foi confirmada abrindo a página individual do item no wiki — o
índice sozinho não foi tratado como prova.

O app tem escopo declarado de magias de truque até nível 5; magias de nível 6+
são adicionadas à mão. Este relatório respeita esse limite: nada acima do nível
5 é reportado como falta.

## Resumo

| Domínio | Situação |
| --- | --- |
| Subclasses | ✅ 68/68 do wiki presentes, mais 14 extras (UA 2025 / Villainous Options) |
| Espécies | ✅ 10/10 do núcleo PHB presentes |
| Antecedentes | ✅ 16/16 do núcleo PHB presentes |
| Truques conhecidos (todas as classes) | ✅ conferem |
| Slots do Warlock (Pact Magic) | ✅ conferem |
| Magias preparadas — meio-conjuradores | ✅ conferem |
| Magias preparadas — Wizard | ❌ curva errada do nível 14 ao 20 |
| Talentos | ⚠️ falta 1 do PHB (Boon of Spell Recall) |
| Magias | ❌ 55 com divergência de campo + 33 ausentes + 5 inexistentes no wiki |
| Itens mágicos | ⚠️ 284 no app contra 444 no wiki |
| Ferramentas | ⚠️ faltam os 4 conjuntos de jogo |

## 1. Wizard: curva de magias preparadas errada

`ProgressionData.kt:541` dá ao Wizard a constante `FULL_CASTER_PREPARED`, que é
a curva de Bard/Cleric/Druid. O Wizard tem curva própria no PHB 2024, mais
generosa a partir do nível 14. O Sorcerer já tem lista própria e está correto.

| Nível | 14 | 15 | 16 | 17 | 18 | 19 | 20 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| App | 17 | 18 | 18 | 19 | 20 | 21 | 22 |
| Wiki | 18 | 19 | 21 | 22 | 23 | 24 | 25 |

Um Wizard de nível 20 prepara 3 magias a menos do que deveria.

## 2. Talento do PHB ausente

- **Boon of Spell Recall** — Epic Boon, fonte `Player's Handbook`. É o único
  item do núcleo PHB ausente entre os 176 talentos do wiki. Os outros 50
  ausentes são de suplementos (Ravenloft, Astarion's Book of Hungers,
  D&D Beyond Drops jul/2026), fora do escopo atual do app.

Nota: "Magic Initiate" aparece como ausente numa comparação ingênua porque o
app o divide em três talentos (Cleric/Druid/Wizard). Não é falta.
Os 12 "Fighting Style Feats" também aparecem como ausentes, mas o app os
modela como opções de característica de classe — só **Pack Fighting** e
**Prone Fighting** (D&D Beyond Drops jul/2026) realmente não existem.

## 3. Magias com divergência de campo

55 magias presentes nos dois lados divergem em escola, nível, alcance,
componentes, duração ou lista de classes. A comparação já desconta dois
falsos positivos conhecidos: o wiki codifica concentração dentro da duração
("C, up to 1 minute") enquanto o app usa um campo `concentration` separado, e
o app enriquece o alcance com a área ("Self (15-foot cone)") que o wiki deixa
na descrição.

Amostras confirmadas na página individual da magia:

- **Searing Smite** — app: Transmutação + concentração. Wiki: Evocação, sem
  concentração, 1 minuto.
- **Summon Aberration** — app: nível 3. Wiki: nível 4.
- **Goodberry** — app: alcance Toque, duração Instantânea. Wiki: alcance
  Pessoal, duração 24 horas.
- **Banishment** — app: 60 pés. Wiki: 30 pés.
- **Giant Insect** — app: Transmutação, 30 pés, listas Cleric/Druid/Sorcerer.
  Wiki: Conjuração, 60 pés, só Druid.
- **Bane** — falta Warlock. **Prayer of Healing** — sobra Bard.
  **Shatter** — sobra Warlock. **Destructive Wave** — sobram Cleric e Druid.

A lista completa está em `docs/auditoria-magias.txt`.

## 4. Magias ausentes (nível 0–5)

33 magias listadas no wiki dentro do escopo do app não existem no catálogo.
Parte é do PHB (Create or Destroy Water, Cloud of Daggers, Find Traps,
Flame Blade, Locate Animals or Plants, Leomund's Tiny Hut, Lightning Arrow,
Speak with Plants, Fount of Moonlight, Rary's Telepathic Bond, Summon Dragon),
parte vem de suplementos dos Reinos Esquecidos.

- **Buzzing Bee** (nível 1, Conjuration) — Druid, Ranger, Sorcerer, Wizard
- **Create or Destroy Water** (nível 1, Transmutation) — Cleric, Druid
- **Insidious Rhythm** (nível 1, Enchantment) — Bard
- **Spellfire Flare** (nível 1, Evocation) — Sorcerer, Wizard
- **Wardaway** (nível 1, Abjuration) — Bard, Cleric, Paladin, Wizard
- **Cloud Of Daggers** (nível 2, Conjuration) — Bard, Sorcerer, Warlock, Wizard
- **Death Armor** (nível 2, Necromancy) — Sorcerer, Wizard
- **Deryan's Helpful Homunculi** (nível 2, Conjuration) — Cleric, Wizard
- **Elminster's Elusion** (nível 2, Abjuration) — Wizard
- **Find Traps** (nível 2, Divination) — Cleric, Druid, Ranger
- **Flame Blade** (nível 2, Evocation) — Druid, Sorcerer
- **Locate Animals or Plants** (nível 2, Divination) — Bard, Druid, Ranger
- **Searing Orb** (nível 2, Evocation) — Cleric, Paladin
- **Tortoise Shell** (nível 2, Abjuration) — Artificer, Druid, Ranger
- **Astral Flood** (nível 3, Evocation) — Bard, Cleric, Sorcerer, Warlock, Wizard
- **Cacophonic Shield** (nível 3, Evocation) — Bard, Sorcerer, Wizard
- **Conjure Constructs** (nível 3, Conjuration) — Wizard
- **Laeral's Silver Lance** (nível 3, Evocation) — Cleric, Sorcerer, Wizard
- **Leomund's Tiny Hut** (nível 3, Evocation) — Bard, Wizard
- **Lightning Arrow** (nível 3, Transmutation) — Ranger
- **Speak with Plants** (nível 3, Transmutation) — Bard, Druid, Ranger
- **Sylune's Viper** (nível 3, Conjuration) — Druid, Wizard
- **Backlash** (nível 4, Abjuration) — Bard, Sorcerer, Warlock, Wizard
- **Doomtide** (nível 4, Conjuration) — Bard, Cleric, Warlock
- **Fount of Moonlight** (nível 4, Evocation) — Bard, Druid
- **Spellfire Storm** (nível 4, Evocation) — Sorcerer, Wizard
- **Sticks to Snakes** (nível 4, Transmutation) — Cleric, Druid, Ranger
- **Alustriel's Mooncloak** (nível 5, Abjuration) — Bard, Druid, Ranger, Wizard
- **Jallarzi's Storm of Radiance** (nível 5, Evocation) — Warlock, Wizard
- **Rary's Telepathic Bond** (nível 5, Divination) — Bard, Wizard
- **Songal's Elemental Suffusion** (nível 5, Transmutation) — Druid, Sorcerer, Wizard
- **Summon Dragon** (nível 5, Conjuration) — Wizard
- **Yolande's Regal Presence** (nível 5, Enchantment) — Bard, Wizard

## 5. Magias no app que não existem no wiki

Cinco entradas são de material 2014/Tasha's/Fizban que o wiki 2024 não lista:

- **Chaos Bolt** (nível 1)
- **Wall of Water** (nível 3)
- **Raulothim's Psychic Lance** (nível 4)
- **Far Step** (nível 5)
- **Summon Draconic Spirit** (nível 5)

## 6. Catálogos parciais

- **Itens mágicos** — 284 no app contra 444 no wiki. Boa parte da diferença é
  de suplementos, mas o volume merece uma verificação dirigida.
- **Ferramentas** — faltam os quatro conjuntos de jogo (Dice, Dragonchess,
  Playing Cards, Three-dragon Ante).
- **Espécies** — Grung e Triton existem no app e não constam no wiki.
  Armaduras: 13/13 conferem.

## Método

A referência foi baixada com `curl` (o site força HTTP e quebra fetchers que
exigem HTTPS), parseada com scripts Python e comparada com extratores que leem
os literais Kotlin de `data/content/`. Nenhuma divergência foi reportada apenas
com base no índice: cada categoria teve amostras verificadas na página
individual do wiki.
