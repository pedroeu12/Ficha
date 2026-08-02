package com.pedroeu.ficha.ui.i18n

/**
 * The interface in Brazilian Portuguese.
 *
 * Keyed by the English original, so a phrase that hasn't been translated yet falls back to
 * reading correctly rather than showing a placeholder. Deliberately covers the interface only
 * — screen titles, buttons, labels, the text that explains what a control does — and not the
 * rules content. A spell's description, a feat's wording, and an item's effect stay in English
 * on purpose: those are quotations from the rulebook, where a loose translation changes what
 * the rule says, and a table needs to be able to check them against the book.
 *
 * Game vocabulary follows the terms Brazilian players actually use at the table — perícia,
 * talento, truque, espaço de magia — rather than a literal rendering of the English.
 */
object PortugueseStrings {

    fun of(text: String): String = TABLE[text] ?: text

    /** Every phrase the interface can show, for a test that checks none has been missed. */
    fun knownKeys(): Set<String> = TABLE.keys

    private val TABLE: Map<String, String> = mapOf(
        // ---------------------------------------------------------------- App and home
        "Ficha" to "Ficha",
        "New Character" to "Novo Personagem",
        "No characters yet" to "Nenhum personagem ainda",
        "Tap New Character to roll up your first adventurer: pick a species, a class, an " +
            "origin, and your ability scores." to
            "Toque em Novo Personagem para criar seu primeiro aventureiro: escolha uma " +
            "espécie, uma classe, uma origem e seus valores de habilidade.",
        "This character will be permanently removed from this device." to
            "Este personagem será removido permanentemente deste aparelho.",
        "Unknown" to "Desconhecido",
        "Level up" to "Subir de nível",
        "Level Up" to "Subir de Nível",
        "Edit sheet" to "Editar ficha",
        "Finish editing" to "Concluir edição",
        "EDIT MODE" to "MODO DE EDIÇÃO",

        // ---------------------------------------------------------------- Appearance and language
        "Appearance" to "Aparência",
        "Appearance: daylight" to "Aparência: luz do dia",
        "Appearance: candlelight" to "Aparência: luz de velas",
        "Follow system" to "Seguir o sistema",
        "Daylight" to "Luz do dia",
        "Candlelight" to "Luz de velas",
        "Language" to "Idioma",
        "Language: English" to "Idioma: inglês",
        "Language: Portuguese" to "Idioma: português",

        // ---------------------------------------------------------------- Backup
        "Backup" to "Backup",
        "Backup and restore" to "Backup e restauração",
        "Back up characters" to "Salvar personagens",
        "Restore from backup" to "Restaurar de um backup",
        "Couldn't write that file." to "Não foi possível gravar esse arquivo.",
        "Couldn't read that file." to "Não foi possível ler esse arquivo.",
        "Nothing needed restoring." to "Não havia nada para restaurar.",

        // ---------------------------------------------------------------- Sheet tabs
        "Stats" to "Atributos",
        "Skills" to "Perícias",
        "Combat" to "Combate",
        "Features" to "Características",
        "Spells" to "Magias",
        "Inventory" to "Inventário",
        "Bio" to "Perfil",

        // ---------------------------------------------------------------- Stats tab
        "Statistics" to "Estatísticas",
        "Ability Scores" to "Valores de Habilidade",
        "Abilities" to "Habilidades",
        "Ability" to "Habilidade",
        "Saving Throws" to "Testes de Resistência",
        "Proficiency" to "Proficiência",
        "Proficiency bonus" to "Bônus de proficiência",
        "Passive Perception" to "Percepção Passiva",
        "Initiative" to "Iniciativa",
        "Speed" to "Deslocamento",
        "Size" to "Tamanho",
        "Size & Speed" to "Tamanho e Deslocamento",
        "AC" to "CA",
        "Armor Class" to "Classe de Armadura",
        "HP" to "PV",
        "Hit Points" to "Pontos de Vida",
        "Hit points" to "Pontos de vida",
        "Temp HP" to "PV Temporários",
        "Hit Dice" to "Dados de Vida",
        "Hit Dice Spent" to "Dados de Vida Gastos",
        "Death Saves" to "Testes contra a Morte",
        "Successes" to "Sucessos",
        "Failures" to "Falhas",
        "HEROIC INSPIRATION" to "INSPIRAÇÃO HEROICA",
        "YES" to "SIM",
        "NO" to "NÃO",
        "Heal 1" to "Curar 1",
        "Take 1 damage" to "Sofrer 1 de dano",
        "Tap to change max HP" to "Toque para mudar os PV máximos",
        "Tap any value to override it, or add a bonus on top of the rules." to
            "Toque em qualquer valor para substituí-lo, ou some um bônus por cima das regras.",
        "adjusted" to "ajustado",
        "adjusted — tap to change" to "ajustado — toque para mudar",
        "tap to add a bonus" to "toque para somar um bônus",
        "mod" to "mod",
        " (proficient)" to " (proficiente)",

        // ---------------------------------------------------------------- Skills tab
        "Expertise" to "Especialização",
        "Skill Proficiency" to "Proficiência em Perícia",
        "Filled pips mark proficiency; a doubled pip marks expertise." to
            "Os círculos preenchidos indicam proficiência; um círculo duplo indica especialização.",
        "Tap a pip to cycle none, proficient, and expertise. Tap a skill name to add a bonus " +
            "or override its total." to
            "Toque em um círculo para alternar entre nenhum, proficiente e especialização. " +
            "Toque no nome de uma perícia para somar um bônus ou substituir o total.",
        "Use a bonus for a temporary or story award from your DM." to
            "Use um bônus para uma recompensa temporária ou de história dada pelo mestre.",
        "Choose two of your skill proficiencies. Your proficiency bonus is doubled for checks " +
            "with them." to
            "Escolha duas de suas proficiências em perícia. Seu bônus de proficiência é " +
            "dobrado nos testes com elas.",
        "Pick your skill proficiencies above first." to
            "Escolha primeiro suas proficiências em perícia acima.",

        // ---------------------------------------------------------------- Combat tab
        "Attack" to "Ataque",
        "Atk" to "Atq",
        "Damage" to "Dano",
        "Name" to "Nome",
        "Weapons & Damage Cantrips" to "Armas e Truques de Dano",
        "Other Attacks & Actions" to "Outros Ataques e Ações",
        "Nothing to attack with yet. Add a weapon on the Inventory tab." to
            "Nada com que atacar ainda. Adicione uma arma na aba Inventário.",
        "Nothing here yet. Add a spell attack, a breath weapon, or anything else you want on " +
            "the sheet." to
            "Nada aqui ainda. Adicione um ataque mágico, uma arma de sopro, ou qualquer " +
            "outra coisa que queira na ficha.",
        "Defenses" to "Defesas",
        "Resistances" to "Resistências",
        "Conditions" to "Condições",
        "Equipped Armor" to "Armadura Equipada",
        "Equipment Training & Proficiencies" to "Treinamento e Proficiências",
        "Armor Training" to "Treinamento em Armadura",
        "Armor" to "Armadura",
        "Weapons" to "Armas",
        "Tools" to "Ferramentas",
        "Tool" to "Ferramenta",
        "Tool Proficiencies" to "Proficiências em Ferramentas",
        "Languages" to "Idiomas",
        "Proficiencies" to "Proficiências",
        "Save DC" to "CD de Resistência",
        "Save DCs by source" to "CDs de Resistência por origem",
        "None" to "Nenhuma",
        "None recorded" to "Nada registrado",
        "Add an Attack" to "Adicionar um Ataque",
        "Edit Attack" to "Editar Ataque",
        "Add attack" to "Adicionar ataque",
        "  Add attack" to "  Adicionar ataque",
        "Attack name" to "Nome do ataque",
        "Attack / damage bonus" to "Bônus de ataque / dano",
        "Damage die" to "Dado de dano",
        "Damage type" to "Tipo de dano",
        "Special effects / notes" to "Efeitos especiais / anotações",
        "Save changes" to "Salvar alterações",
        "On a hit, the target must succeed on a DC 15 save or…" to
            "Se acertar, o alvo precisa passar em um teste CD 15 ou…",
        "Fire" to "Fogo",
        "e.g. 2" to "ex.: 2",

        // ---------------------------------------------------------------- Resources
        "Limited Uses" to "Usos Limitados",
        "Comes back on" to "Recupera em",
        "What comes back" to "O que recupera",
        "How to spend it" to "Como gastar",
        "Spend 1" to "Gastar 1",
        "Give back" to "Devolver 1",
        "Nothing else is spent right now." to "Nada mais está gasto no momento.",
        "Nothing with a limited number of uses yet. Level up, or add your own tracker." to
            "Nada com número limitado de usos ainda. Suba de nível, ou crie seu próprio contador.",
        "Track Something Else" to "Acompanhar Outra Coisa",
        "  Track something else" to "  Acompanhar outra coisa",
        "Add tracker" to "Adicionar contador",
        "Number of uses" to "Número de usos",
        "For any ability with a set number of uses that the app doesn't already list." to
            "Para qualquer habilidade com um número fixo de usos que o app ainda não lista.",
        "Set a maximum of 0 to hide this tracker entirely." to
            "Defina o máximo como 0 para esconder este contador por completo.",
        "Refills on a Short Rest and a Long Rest." to
            "Recupera em um Descanso Curto e em um Descanso Longo.",
        "Refills only on a Long Rest." to "Recupera apenas em um Descanso Longo.",
        "Rests leave it alone; you reset it yourself." to
            "Os descansos não o afetam; você mesmo o reinicia.",
        "  Hide details" to "  Ocultar detalhes",
        "  What this does" to "  O que isso faz",

        // ---------------------------------------------------------------- Spells tab
        "Spellcasting" to "Conjuração",
        "Spell Slots" to "Espaços de Magia",
        "Spell slots" to "Espaços de magia",
        "Prepared & Known Spells" to "Magias Preparadas e Conhecidas",
        "Prepared spells" to "Magias preparadas",
        "Prepared" to "Preparada",
        "Not prepared" to "Não preparada",
        "Always prepared" to "Sempre preparada",
        "Cantrip" to "Truque",
        "Cantrips" to "Truques",
        "Cantrips known" to "Truques conhecidos",
        "Spells learned" to "Magias aprendidas",
        "New Spells" to "Novas Magias",
        "New Cantrips" to "Novos Truques",
        "Add a Spell" to "Adicionar uma Magia",
        "Add spell" to "Adicionar magia",
        "  Add a spell" to "  Adicionar uma magia",
        "Add a spell not listed" to "Adicionar uma magia fora da lista",
        "Or add a spell by name" to "Ou adicione uma magia pelo nome",
        "Spell name" to "Nome da magia",
        "Search spells" to "Buscar magias",
        "My class list" to "Minha lista de classe",
        "All levels" to "Todos os níveis",
        "All" to "Tudo",
        "Added by hand" to "Adicionada à mão",
        "Added by hand." to "Adicionada à mão.",
        "No spells recorded yet. Level up to learn some, or add them here in Edit Mode." to
            "Nenhuma magia registrada ainda. Suba de nível para aprender algumas, ou " +
            "adicione-as aqui no Modo de Edição.",
        "This character has no spellcasting from their class. You can still add spells from " +
            "feats, items, or anywhere else." to
            "Este personagem não conjura magias pela classe. Você ainda pode adicionar " +
            "magias vindas de talentos, itens ou de qualquer outra fonte.",
        "Tap to swap which spells you have ready for the day." to
            "Toque para trocar quais magias você deixa prontas para o dia.",
        "Casting Time" to "Tempo de Conjuração",
        "Range" to "Alcance",
        "Components" to "Componentes",
        "Duration" to "Duração",
        "Concentration" to "Concentração",
        "Ritual" to "Ritual",
        "From" to "De",
        "Effect" to "Efeito",
        "A spell attack roll" to "Uma jogada de ataque mágico",
        "Resolves with" to "Resolve com",
        "No description recorded for this spell." to "Nenhuma descrição registrada para esta magia.",
        "This spell isn't in the rulebook data, so only what you entered is shown. You can " +
            "edit its text in Edit Mode." to
            "Esta magia não está nos dados do livro, então só aparece o que você digitou. " +
            "Você pode editar o texto no Modo de Edição.",
        "Yes" to "Sim",
        "No" to "Não",

        // ---------------------------------------------------------------- Inventory tab
        "Equipment" to "Equipamento",
        "  Add an item" to "  Adicionar um item",
        "Add to inventory" to "Adicionar ao inventário",
        "Nothing carried yet. Add gear from the rulebook, or write in your own." to
            "Nada sendo carregado ainda. Adicione equipamento do livro, ou escreva o seu.",
        "Equipped" to "Equipado",
        "Carried" to "Carregado",
        "Made" to "Criado",
        "Tap for details" to "Toque para ver os detalhes",
        "Item name" to "Nome do item",
        "Item notes" to "Anotações do item",
        "Quantity" to "Quantidade",
        "Weight" to "Peso",
        "Weight (lb)" to "Peso (lb)",
        "Cost" to "Custo",
        "Capacity" to "Capacidade",
        "Custom item" to "Item personalizado",
        "From the rulebook" to "Do livro de regras",
        "Search the rulebook" to "Buscar no livro de regras",
        "Nothing matches. Add it by name below." to "Nada corresponde. Adicione pelo nome abaixo.",
        "No description recorded for this item. Add one in Edit Mode, or replace it with an " +
            "entry from the rulebook list." to
            "Nenhuma descrição registrada para este item. Escreva uma no Modo de Edição, ou " +
            "substitua-o por uma entrada da lista do livro.",
        "Tap to add your own notes" to "Toque para escrever suas anotações",
        "Coins" to "Moedas",
        "CP" to "PC",
        "SP" to "PP",
        "EP" to "PE",
        "GP" to "PO",
        "PP" to "PL",
        "Starting Gold" to "Ouro Inicial",

        // ---------------------------------------------------------------- Features tab
        "Feats" to "Talentos",
        "Feat" to "Talento",
        "Origin Feat" to "Talento de Origem",
        "No feats yet." to "Nenhum talento ainda.",
        "  Add feat" to "  Adicionar talento",
        "Add a Feat" to "Adicionar um Talento",
        "Search feats" to "Buscar talentos",
        "Already taken" to "Já escolhido",
        "Make every choice above first" to "Faça primeiro todas as escolhas acima",
        "Other Features" to "Outras Características",
        "  Add feature" to "  Adicionar característica",
        "New feature" to "Nova característica",
        "Feature name" to "Nome da característica",
        "Feature text" to "Texto da característica",
        "Anything the app doesn't already know about goes here." to
            "Tudo o que o app ainda não conhece entra aqui.",
        "Origin Choices" to "Escolhas de Origem",
        "What you picked from your species, class, background, and feats." to
            "O que você escolheu na sua espécie, classe, antecedente e talentos.",
        "Traits" to "Traços",
        "Level 1 Features" to "Características de Nível 1",
        "Features gained" to "Características ganhas",
        "Change" to "Mudar",
        "Choose" to "Escolher",
        "not chosen yet" to "ainda não escolhido",
        "Tap to add a description" to "Toque para escrever uma descrição",
        "You may change these while you rest" to "Você pode trocar estas ao descansar",
        "Read more" to "Ler mais",
        "Show less" to "Mostrar menos",

        // ---------------------------------------------------------------- Replicate Magic Item
        "Replicate Magic Item" to "Replicar Item Mágico",
        "You make these when you finish a Long Rest, with Tinker's Tools in hand. Anything " +
            "you make goes into your inventory, and a weapon or armor made this way reaches " +
            "your attacks and Armor Class on its own." to
            "Você cria estes ao terminar um Descanso Longo, com Ferramentas de Funileiro em " +
            "mãos. Tudo o que você criar vai para o seu inventário, e uma arma ou armadura " +
            "criada assim chega sozinha aos seus ataques e à sua Classe de Armadura.",
        "No plans learned yet. They're chosen with the Replicate Magic Item feature at " +
            "levels 2, 6, 10, 14, and 18." to
            "Nenhum projeto aprendido ainda. Eles são escolhidos com a característica " +
            "Replicar Item Mágico nos níveis 2, 6, 10, 14 e 18.",
        "Make" to "Criar",
        "Make another" to "Criar outro",
        "Make — choose an item" to "Criar — escolha um item",
        "No room today" to "Sem espaço hoje",
        "Set aside" to "Guardar",

        // ---------------------------------------------------------------- Bio tab
        "Identity" to "Identidade",
        "Character name" to "Nome do personagem",
        "Character Name" to "Nome do Personagem",
        "Species" to "Espécie",
        "Lineage" to "Linhagem",
        "Class" to "Classe",
        "Subclass" to "Subclasse",
        "Origin" to "Origem",
        "Level" to "Nível",
        "Alignment" to "Tendência",
        "Lawful Good" to "Leal e Bom",
        "Neutral Good" to "Neutro e Bom",
        "Chaotic Good" to "Caótico e Bom",
        "Lawful Neutral" to "Leal e Neutro",
        "True Neutral" to "Neutro",
        "Chaotic Neutral" to "Caótico e Neutro",
        "Lawful Evil" to "Leal e Mau",
        "Neutral Evil" to "Neutro e Mau",
        "Chaotic Evil" to "Caótico e Mau",
        "Appearance (optional)" to "Aparência (opcional)",
        "Backstory & Personality" to "História e Personalidade",
        "Backstory & Personality (optional)" to "História e Personalidade (opcional)",
        "Session Notes" to "Anotações de Sessão",
        "Your notes" to "Suas anotações",
        "Notes (optional)" to "Anotações (opcional)",

        // ---------------------------------------------------------------- Rests
        "Short Rest" to "Descanso Curto",
        "Long Rest" to "Descanso Longo",
        "Short rest" to "Descanso curto",
        "Long rest" to "Descanso longo",
        "Rest complete" to "Descanso concluído",
        "Rest without spending Hit Dice" to "Descansar sem gastar Dados de Vida",
        "Hit points restored to full" to "Pontos de vida restaurados por completo",
        "All spell slots" to "Todos os espaços de magia",
        "Pact Magic spell slots" to "Espaços de magia da Magia de Pacto",
        "Spell slots restored" to "Espaços de magia recuperados",
        "Death saves cleared" to "Testes contra a morte zerados",
        "  Roll" to "  Rolar",
        "Roll the die" to "Rolar o dado",
        "Reroll" to "Rolar de novo",
        "Clear dice" to "Limpar dados",
        "Not rolled yet" to "Ainda não rolado",
        "Take the average" to "Usar a média",
        "Average" to "Média",
        "Enter it yourself" to "Digitar você mesmo",
        "Enter a number yourself, for tables with their own house rule." to
            "Digite um número você mesmo, para mesas com regra própria.",

        // ---------------------------------------------------------------- Creation and level up
        "Choose a Species" to "Escolha uma Espécie",
        "Choose an Origin" to "Escolha uma Origem",
        "Choose a Class" to "Escolha uma Classe",
        "Choose a Subclass" to "Escolha uma Subclasse",
        "Class Options" to "Opções de Classe",
        "Options" to "Opções",
        "Origin Options" to "Opções de Origem",
        "Grants" to "Concessões",
        "Name & Details" to "Nome e Detalhes",
        "Details" to "Detalhes",
        "New Features" to "Novas Características",
        "New Class Features" to "Novas Características de Classe",
        "New Subclass Features" to "Novas Características de Subclasse",
        "Ability Scores or Feat" to "Valores de Habilidade ou Talento",
        "Improve" to "Melhorar",
        "Feat Options" to "Opções de Talento",
        "Review" to "Revisão",
        "What You Gain" to "O Que Você Ganha",
        "Epic Boons" to "Dádivas Épicas",
        "Your species shapes your size, speed, senses, and the traits you were born with." to
            "Sua espécie define seu tamanho, deslocamento, sentidos e os traços com que você nasceu.",
        "Your origin is the life you led before adventuring. It grants ability score " +
            "increases, two skills, a tool, and an origin feat." to
            "Sua origem é a vida que você levou antes de se aventurar. Ela concede aumentos " +
            "de valor de habilidade, duas perícias, uma ferramenta e um talento de origem.",
        "Your class is what you do in the world: how you fight, what magic you wield, and " +
            "what you're trained in. You'll pick its options on the next step." to
            "Sua classe é o que você faz no mundo: como luta, que magia empunha e em que é " +
            "treinado. Você escolherá as opções dela no próximo passo.",
        "These come from your species, class, and origin. Each one is yours to pick." to
            "Estas vêm da sua espécie, classe e origem. Cada uma é sua para escolher.",
        "Your species, class, and origin didn't leave any further choices open. Continue to " +
            "your ability scores." to
            "Sua espécie, classe e origem não deixaram mais nenhuma escolha em aberto. " +
            "Siga para os valores de habilidade.",
        "Skills your species or origin already grants are shown as unavailable, so a pick is " +
            "never wasted on a duplicate." to
            "As perícias que sua espécie ou origem já concede aparecem indisponíveis, para " +
            "que nenhuma escolha seja desperdiçada em uma repetição.",
        "Already gained from another source" to "Já obtido por outra fonte",
        "Classes you already have" to "Classes que você já tem",
        "Start a new class" to "Começar uma nova classe",
        "Put this level into a class" to "Coloque este nível em uma classe",
        "Pick a species and any options it offers" to
            "Escolha uma espécie e as opções que ela oferecer",
        "Pick an origin and assign its ability bonuses" to
            "Escolha uma origem e distribua os bônus de habilidade dela",
        "Pick a class" to "Escolha uma classe",
        "Pick your subclass" to "Escolha sua subclasse",
        "Complete every option below" to "Complete todas as opções abaixo",
        "Complete every choice below" to "Complete todas as escolhas abaixo",
        "Complete every choice your feat asks for" to
            "Complete todas as escolhas que seu talento pede",
        "Resolve every grant your origin left open" to
            "Resolva todas as concessões que sua origem deixou em aberto",
        "Assign all six ability scores" to "Distribua os seis valores de habilidade",
        "Assign both points, or choose a feat" to "Distribua os dois pontos, ou escolha um talento",
        "Choose how to gain hit points" to "Escolha como ganhar pontos de vida",
        "Choose your new spells" to "Escolha suas novas magias",
        "Give your character a name" to "Dê um nome ao seu personagem",
        "Ready to create your character" to "Pronto para criar seu personagem",
        "Confirm to save your new level" to "Confirme para salvar seu novo nível",
        "Looks good" to "Está certo",
        "Choose 1" to "Escolha 1",
        "One skill of your choice" to "Uma perícia à sua escolha",
        "no extra proficiencies" to "sem proficiências adicionais",
        "Generation Method" to "Método de Geração",
        "Available Scores" to "Valores Disponíveis",
        "Final Scores (with origin bonuses)" to "Valores Finais (com os bônus de origem)",
        "All scores assigned." to "Todos os valores distribuídos.",
        "Points remaining" to "Pontos restantes",
        "Tap an ability to assign points; tap it again to take them back." to
            "Toque em uma habilidade para atribuir pontos; toque de novo para recuperá-los.",
        "Choose how to spread the increase across this origin's three abilities, then tap " +
            "the abilities to assign." to
            "Escolha como distribuir o aumento entre as três habilidades desta origem, " +
            "depois toque nas habilidades para atribuir.",
        "+2 to one ability" to "+2 em uma habilidade",
        "+1 to two abilities" to "+1 em duas habilidades",
        "Take a feat instead" to "Pegar um talento no lugar",
        "Raise a single ability by 2, up to a maximum of 20." to
            "Aumente uma única habilidade em 2, até o máximo de 20.",
        "Raise two different abilities by 1 each." to
            "Aumente duas habilidades diferentes em 1 cada.",
        "Skip the increase and take a feat instead." to
            "Abra mão do aumento e pegue um talento no lugar.",
        "Add a tool or proficiency" to "Adicionar uma ferramenta ou proficiência",
        "Add tool" to "Adicionar ferramenta",

        // ---------------------------------------------------------------- Edit mode and dialogs
        "Edit" to "Editar",
        "Save" to "Salvar",
        "Cancel" to "Cancelar",
        "Done" to "Concluído",
        "Delete" to "Excluir",
        "Add" to "Adicionar",
        "Create" to "Criar",
        "Back" to "Voltar",
        "Next" to "Próximo",
        "OK" to "OK",
        "Clear" to "Limpar",
        "Reset" to "Restaurar",
        "Reset all" to "Restaurar tudo",
        "Reset to the class table" to "Voltar à tabela da classe",
        "Reset puts the original rulebook text back." to
            "Restaurar devolve o texto original do livro de regras.",
        "Selected" to "Selecionado",
        "Search" to "Buscar",
        "Description" to "Descrição",
        "Description or notes" to "Descrição ou anotações",
        "Bonus (added to the rules)" to "Bônus (somado às regras)",
        "Override (replaces everything)" to "Substituição (troca tudo)",
        "Leave blank to use the rules" to "Deixe em branco para usar as regras",
        "Not set" to "Não definido",

        // ---------------------------------------------------------------- Phrases with values
        // Numbered placeholders, so word order can differ from the English.
        "Level {0}" to "Nível {0}",
        "Level {0} — {1}" to "Nível {0} — {1}",
        "Level {0} {1} {2}" to "Nível {0} {1} {2}",
        "Level 1 {0} {1}" to "{0} {1} de Nível 1",
        "Level {0} grants an Epic Boon feat." to
            "O nível {0} concede um talento de Dádiva Épica.",
        "Class Features — {0}" to "Características de Classe — {0}",
        "Subclass — {0}" to "Subclasse — {0}",
        "Species Traits — {0}" to "Traços de Espécie — {0}",
        "Playtest material — {0}" to "Material de playtest — {0}",
        "Mastery: {0}" to "Maestria: {0}",
        "DC {0}" to "CD {0}",
        "Proficiency {0}" to "Proficiência {0}",
        "+{0} temporary" to "+{0} temporários",
        "+{0} max HP" to "+{0} de PV máximos",
        "Prepared spells  {0} / {1}" to "Magias preparadas  {0} / {1}",
        "Delete {0}?" to "Excluir {0}?",
        "{0} items • {1} lb" to "{0} itens • {1} lb",
        "{0} — maximum uses" to "{0} — usos máximos",
        "  Filter: {0}" to "  Filtro: {0}",
        "  Roll d{0}" to "  Rolar d{0}",
        "Hit points rolled (1-{0})" to "Pontos de vida rolados (1-{0})",
        "The rules give {0}." to "As regras dão {0}.",
        "Granted skill: {0}" to "Perícia concedida: {0}",
        "Origin Feat: {0}" to "Talento de Origem: {0}",
        "Origin bonus +{0}" to "Bônus de origem +{0}",
        "Unassigned: {0}" to "Não distribuído: {0}",
        "{0} of {1} made" to "{0} de {1} criados",
        " — room for {0} more." to " — espaço para mais {0}.",
        "Made: {0}" to "Criado: {0}",
        "subclass" to "subclasse",
        "Your {0} shapes the rest of your career. This choice is permanent." to
            "Sua {0} molda o resto da sua carreira. Esta escolha é permanente.",
        "{0} decisions. These lock in what your character is trained in and what they can do " +
            "at level 1." to
            "Decisões de {0}. Elas definem em que seu personagem é treinado e o que ele pode " +
            "fazer no nível 1.",
        "Reaching level {0} adds a Hit Die roll plus your Constitution modifier ({1}) to your " +
            "maximum hit points." to
            "Chegar ao nível {0} soma uma rolagem de Dado de Vida mais o seu modificador de " +
            "Constituição ({1}) aos seus pontos de vida máximos.",
        "You can cast up to level {0}. Spells above level 5 aren't in the app's catalog yet, " +
            "so type the name to add it." to
            "Você conjura até o nível {0}. Magias acima do nível 5 ainda não estão no " +
            "catálogo do app, então digite o nome para adicioná-la.",
        "You are {0}. Continue in one of those, or start a new class if your scores allow it." to
            "Você é {0}. Continue em uma dessas, ou comece uma nova classe se seus valores " +
            "permitirem.",
    )
}
