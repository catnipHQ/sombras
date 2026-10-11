# Veritates 1.2.0

Mod **Veritates** para Minecraft **Forge 1.20.1**. Adiciona o **Livro das Sombras**, que invoca a Espada de Sangue, e o **Livro do Sangue**, a versão melhorada dele, que deixa você escolher entre quatro armas de sangue ou invocar um **Ser de Sangue**. Desde a 1.1.0 há um segundo caminho, o **Livro do Tempo**, e desde a 1.2.0 um terceiro, o **Livro dos Espíritos**.

## Livros

| Item | Descrição |
|------|-----------|
| Livro das Sombras | Botão direito invoca a Espada de Sangue. |
| Livro do Tempo | O caminho do tempo: livro preto com um relógio branco na capa. Funciona igual ao Livro do Sangue (duas seções, roda R, rituais preparados, 5 usos), com o menu em preto e cinza, e **paga em fome** em vez de vida. Por enquanto só existe no modo criativo. Veja abaixo. |
| Livro dos Espíritos | O caminho das almas: livro de capa azul. Mesmas regras dos outros (duas seções, roda R, rituais preparados, 5 usos), com o menu em azul-claro, partículas de **fogo azul** (fogo de alma) e sons de almas. **Paga em níveis de experiência.** Por enquanto só no modo criativo. Veja abaixo. |
| Livro do Sangue | Botão direito abre o livro em duas seções: **I. Invocações** (espada, lança, martelo, arco ou Ser de Sangue, como antes) e **II. Rituais menores** (feitiços de sangue, veja abaixo). Não substitui o Livro das Sombras, os dois existem. |

### Seções travadas e roda de seleção

Ao escolher uma seção, o livro fica **travado** nela: o botão direito passa a abrir direto essa seção. Para trocar, use **‹ Voltar** embaixo da lista (ou a tecla Backspace).

Com uma seção travada e o livro na mão, a tecla **R** abre uma **roda de seleção**: mova o mouse na direção da opção e solte o R (ou clique) para escolher. Na roda das Invocações a escolha invoca na hora; na dos Rituais ela prepara o ritual. Apertar R de novo sem escolher fecha a roda. Sem seção travada, o R abre o menu normal. A tecla pode ser trocada em Opções > Controles, na categoria "Veritates".

## Armas

| Arma | Dano | Particularidades |
|------|------|------------------|
| Espada de Sangue | 7 | Igual à espada de diamante (velocidade -2.4, 1561 de durabilidade). Cada golpe causa **Hemorragia** por 4 s: meio coração de dano por segundo e um rastro de sangue no chão enquanto o alvo anda, que some depois de alguns segundos. Golpes novos renovam o tempo, não acumulam. |
| Lança de Sangue | 6 | Segure o botão direito e solte para arremessar como um tridente. Ela volta sozinha para você depois de acertar ou de cravar em um bloco. O arremesso aplica Lentidão por 2 s e deixa o alvo sangrando pelo mesmo tempo (o golpe corpo a corpo não). |
| Martelo de Sangue | 10 | Lento (velocidade de ataque 0.6). Segure o botão direito para carregar (1 s) e solte: onda de choque que acerta todos em até 4 blocos com 5 de dano e empurra. Cada golpe custa 3 pontos de vida (1,5 coração). Recarga de 2 s. |
| Arco de Sangue | 7 | Não gasta flechas, mas cada flecha custa 1 coração. 7 de dano com o arco totalmente puxado, e o tiro totalmente puxado é crítico (bônus aleatório do Minecraft, até 11). A flecha não pode ser recolhida. |

### Criaturas sem sangue

Esqueletos (de qualquer tipo), o Wither, cavalos esqueleto e os Esqueletinhos do Tempo são feitos de osso: **não sangram**. A Espada de Sangue não causa Hemorragia neles, a Lança não os deixa sangrando, e o **Dilacerar** não é lançado neles (nada é cobrado). O Corte sangrento ainda os acerta, mas solta lascas de osso em vez de sangue. Zumbis e piglins zumbificados continuam sangrando. A lista é a tag de entidades `livro_das_sombras:sem_sangue`, que um datapack pode ampliar.

## Rituais menores (Livro do Sangue)

Escolher um ritual no menu **prepara** o livro: aparece um olho na capa, ele brilha e o nome muda para "Livro do Sangue · Dilacerar". Com o livro preparado:

- **Botão direito** lança o ritual. O livro continua preparado, então dá para lançar de novo quando a recarga acabar.
- **Shift + botão direito** cancela e o livro volta ao normal (e o botão direito volta a abrir o menu).

Os rituais custam vida (nunca o suficiente para matar) e **cada lançamento gasta um uso do livro**, como as invocações. Cada ritual tem sua própria recarga, que é sua (não do livro): enquanto recarrega, aparece uma **barrinha vermelha com o ícone do ritual no canto inferior esquerdo da tela**, sempre visível, e o menu e a roda mostram a mesma barra. Tentar lançar antes da hora só faz um clique abafado. A recarga zera quando você morre. No modo criativo os rituais não custam vida, usos nem recarga.

| Ritual | Custo | Efeito | Recarga |
|--------|-------|--------|---------|
| Dilacerar | 2,5 ♥ | Mire em uma criatura ou jogador a até 8 blocos. Depois de 1 s de aviso (sangue girando em volta do alvo), ele sofre 1 de dano a cada meio segundo por 6 s: 6 ♥ no total, ignorando armadura. Se ele morrer antes, acaba. | 20 s |
| Troca de sangue | 3 ♥ | Mire em um jogador ou criatura a até 8 blocos: ele recupera 4 ♥. Um fio de sangue liga vocês. Não funciona se ele já estiver com a vida cheia (nada é cobrado). | 10 s |
| Corte sangrento | 4 ♥ | Um corte de sangue sai em linha reta por 10 blocos (para na primeira parede) e acerta tudo no caminho com 7 ♥ de dano (a armadura reduz) e um empurrão. Não acerta os seus animais nem o seu Ser de Sangue. | 40 s |

Sem alvo na mira, Dilacerar e Troca de sangue não são lançados e nada é cobrado (nem vida, nem uso do livro).

## Livro do Tempo

Mesmo funcionamento do Livro do Sangue (seções travadas, roda R, Shift + botão direito cancela o ritual preparado, 5 usos, recarga de 5 s nas invocações), mas o menu é preto e cinza, com um mostrador de relógio no fundo, e o preço é **fome** (2 pontos = 1 coxinha). Cada invocação dá 2 s de **Lentidão** em vez de Náusea. Ainda não dá para fabricar: só pela aba criativa.

### Invocações

| Invocação | Custo | Efeito |
|-----------|-------|--------|
| Esqueletinhos | 3 de fome | Três esqueletos bebês com arco, por **4 minutos** (o tempo aparece como efeito no inventário). Um pouco mais fortes que um esqueleto: 22 de vida (esqueleto: 20), 2 de armadura, mais rápidos e flechas com +0,5 de dano. Seguem e protegem você como o Ser de Sangue (atiram em quem você atacar e em quem atacar você), sem modo de guarda nem menu. Não queimam no sol. As flechas deles atravessam você, seus animais e suas criaturas. Um grupo por vez; somem se você morrer ou ficar fora por mais de 5 s. |
| Foice do Tempo | 1,5 de fome | Dano baixo, igual à enxada de diamante (1 de dano, e ara a terra como enxada). **Botão direito em plantações** adianta o tempo em todas as plantas numa área 3×3 (um bloco acima e abaixo também): cada planta tem **50% de chance de crescer na hora e 50% de morrer**. Vale para trigo, cenoura, batata, beterraba, caules de abóbora e melancia, frutas doces, cacau e fungo do Nether. Depois do uso a foice some e só pode ser invocada de novo depois de **2 minutos** (barra cinza no canto da tela). |
| Relógio Negro | 2,5 de fome | **3 cargas.** Botão direito mirando uma criatura a até 12 blocos: ela fica **parada no tempo por 2 s** (não anda, não olha, não ataca; um creeper prestes a explodir desarma). Em **jogadores e chefes** (Wither, Dragão do Fim) só dá **Lentidão III por 3 s**. 3 s de recarga entre cargas; depois da última, o relógio some. |

A Foice e o Relógio seguem as regras das armas de sangue: uma arma por vez (somando os dois livros), somem se forem largadas, guardadas em baú ou se você morrer.

### Rituais menores

| Ritual | Custo | Efeito | Recarga |
|--------|-------|--------|---------|
| Lentidão | 1 de fome | Mire em uma criatura ou jogador a até 12 blocos. Leva **1 s para conjurar** (um mostrador de partículas se fecha em volta do alvo); depois dá **Lentidão II por 8 s** no alvo e em quem estiver a até 2,5 blocos dele (menos você e suas criaturas). | 15 s |
| Murchar | 2 de fome | Mire em um alvo a até 10 blocos. Leva **1,5 s para conjurar**; depois dá **Murchar II por 6 s** (6 de dano no total) e **Fraqueza por 6 s**. | 25 s |
| Reverter Passos | marcar: grátis · voltar: 1,5 de fome | O primeiro uso **marca o lugar** onde você está. O próximo uso **leva você de volta** à marca. A marca vale por 5 minutos e só funciona a até **25 blocos**, na mesma dimensão. A partir de **20 blocos** aparece um aviso e toca um som grave; passando de 25 a marca se desfaz. Só você vê a marca (partículas cinza no chão) e, no canto da tela, a distância e o tempo restante. Marcar não gasta uso do livro nem inicia a recarga; voltar sim. | 30 s |

Se o alvo morrer, sumir ou se afastar demais durante a conjuração, o ritual se desfaz (o custo já foi pago). Como a marca do Reverter Passos fica com você, dá para marcar, trocar para outro ritual na roda, lutar, e voltar para o Reverter Passos depois.

## Livro dos Espíritos

Mesmo funcionamento dos outros livros (seções travadas, roda R, Shift + botão direito cancela o ritual preparado, 5 usos, recarga de 5 s nas invocações). O menu é azul-claro, com almas em espiral no fundo, e o preço é **níveis de experiência**. Cada invocação dá 2 s de **Escuridão** em vez de Náusea. Todas as criaturas seguem e protegem você como o Ser de Sangue (atacam quem você ataca e quem te ataca, e vão atrás de monstros hostis por perto) e nenhum ataque seu, de animais seus ou de outras invocações machuca o seu lado. Um grupo de cada tipo por vez; somem se você morrer ou ficar fora por mais de 5 s.

### Invocações

| Invocação | Custo | Recarga | Efeito |
|-----------|-------|---------|--------|
| Invocar Vex | 3 níveis | 30 s | **2 Vex espirituais** por **45 s**. Vex comum melhorado: 18 de vida (vex: 14), 5 de dano (vex: 4), atravessam paredes e voam. É o papel de dano da seita. |
| Invocar Aliados | 3 níveis | 30 s | **3 allays** (os bichinhos azuis do Minecraft) por **60 s**. 16 de vida, seguem você voando. Não atacam: a cada segundo aplicam **Fraqueza por 5 s** e **Lentidão por 4 s** nos monstros perto deles. Não dá para entregar itens a eles. Papel de utilidade, não de dano. |
| Invocar Golem de Almas | 6 níveis | 90 s | Um golem de vidro azul, do tamanho do Golem de Ferro, que **dura até morrer**. 120 de vida (golem: 100), 4 de armadura, 16 de dano (golem: 7 a 21). Recupera vida aos poucos fora de combate. **Shift + botão direito** nele (só o dono, a até 10 blocos) abre o menu de formas. |

#### Menu do Golem de Almas

| Opção | Efeito |
|-------|--------|
| I. Colosso | O golem passa a **carregar** cada ataque: levanta os braços por 1,5 s, com partículas de alma subindo e um anel de fogo azul se fechando, e então **desfere um golpe em área** (raio de 5 blocos, 18 de dano, joga os inimigos para cima). Recarga de 2 s entre golpes. Escolher de novo volta ao normal. |
| II. Forma Destruidora | Você **sacrifica 5 corações** (vida máxima) **até o golem morrer**, ou até você voltar à forma normal. Em troca o golem fica maior e escuro, com **+80 de vida, +8 de armadura, +4 de resistência de armadura (dureza), +6 de dano e Resistência I**. Exige vida máxima de pelo menos 6 corações. Só uma Forma Destruidora por vez. |
| III. Morra | Mata o golem na hora (devolve os corações, se houver sacrifício). |

### Rituais menores

| Ritual | Custo | Efeito | Recarga |
|--------|-------|--------|---------|
| Portal | 2 níveis | **Botão direito** abre o **primeiro portal** (grátis) onde você mira (até 32 blocos). **Botão direito de novo** abre o **segundo** (ao menos 4 blocos de distância), que liga os dois e é quando cobra o preço. Qualquer um (jogador, criatura, item, flecha) que atravessa um sai pelo outro, para o lado oposto, mantendo o movimento. Ficam abertos até você fechar: **Shift + botão direito olhando para um portal** com o livro na mão fecha o portal (e o par). Também fecham se você sair do jogo ou morrer, ou ao abrir um novo par. Cada coisa só passa uma vez a cada 0,5 s. | 25 s |
| Fogo das Almas | 2 níveis | Uma **rajada de chamas azuis** em cone à sua frente (alcance de 7 blocos, 60° de abertura). Quem for atingido leva **6 de dano (3 corações)** de magia e pega fogo por 3 s. Não passa por paredes. Poupa você e suas criaturas. | 12 s |
| Troca | 1 nível | Mire em um ser vivo a até 24 blocos: vocês **trocam de lugar**, com um rastro de almas entre os dois. Não funciona em chefes (Wither, Dragão do Fim) nem quando não há espaço no destino. | 8 s |

## Caldeirão Ritualístico

É assim que se fabricam os livros. Receita (bancada):

```
Lingote de ferro   ·              Lingote de ferro
Obsidiana          ·              Obsidiana
Bloco de ferro     Obsidiana      Bloco de ferro
```

1. **Acenda** o caldeirão com um isqueiro (pederneira e aço).
2. **Botão direito** nele: você perde **4 corações** (8 de vida) e ele enche um pouco de sangue. O chão em volta fica **manchado de sangue** e aparece no chat: *"O sangue busca por um sacrifício"*.
3. **Mate uma criatura** em cima do caldeirão ou a até 3 blocos dele (com a mão, uma arma ou flecha; jogadores e suportes de armadura não contam). Aparece no chat: *"Ele busca pela carne"*.
4. **Jogue carne** dentro dele, **uma peça por vez** (tecla Q em cima, ou botão direito com a carne na mão). Se cair uma pilha inteira, ele come uma peça e cospe o resto para fora. Qualquer carne serve, inclusive carne podre; quanto mais saciedade a carne dá, mais ela enche. Cada peça vale 1 + a saciedade dela, e o caldeirão completa com 60. Na prática: uns 34 de carne podre, 22 de carne crua de vaca, 8 de frango assado ou 5 de bife. Antes de completar, o botão direito mostra *"Precisa de mais carne"* com a porcentagem.
5. Ao chegar em 100%, aparece no chat *"O caldeirão está completo."*. **Botão direito** abre o menu: **Livro do Sangue** ou **Livro das Sombras**. Escolher um livro completa o ritual: você fica com **Náusea e Escuridão por 5 s**, e o caldeirão esvazia, apaga e as manchas de sangue somem; para outro livro, faça o ritual de novo.

Quebre com picareta de ferro ou melhor. Ao quebrar, o progresso se perde: ele volta vazio e apagado.

## Ser de Sangue

Criatura invocada pelo Livro do Sangue (mesmo custo: 5 corações e Náusea).

- Como um zumbi, só que mais forte: 26 de vida (zumbi: 20) e 5 de dano (zumbi: 3). Não queima no sol.
- Dura **2 minutos** ou até a vida chegar a 0. O tempo restante aparece no inventário (tecla **E**) como um efeito de status.
- Segue e defende você como um lobo: ataca quem você atacar e quem atacar você.
- **Botão direito** nele ativa o **modo de guarda** (mensagem no chat): ele fica parado no lugar, mas continua revidando se você atacar ou for atacado, e depois volta ao posto. Clique de novo para ele voltar a seguir.
- Nunca ataca quem o invocou (nem os seus animais domesticados).
- **Shift + botão direito** nele (só quem o invocou) abre o menu ritual:
  1. **Prolongar:** custa 9,5 corações e corta 2 corações da sua vida máxima até ele morrer (acumula a cada uso); o tempo restante volta para 10 minutos.
  2. **Morra:** mata a criatura na hora.
  3. **Fortalecer:** 3 corações saem da sua barra de vida (vida máxima) até ele morrer. Em troca ele ganha +5 de dano, +6 de armadura, +2 de resistência de armadura e resiste a empurrões. Uma vez por criatura.
- Só um Ser de Sangue por vez. Ele se dissipa se você morrer, ou se você sair do jogo ou trocar de dimensão por mais de 5 s.

## Regras de invocação (valem para os três livros)

- Cada livro tem **5 usos** (a barra de durabilidade e a dica do item mostram quantos restam). Invocações e rituais menores que dão certo gastam um uso cada. No último uso o livro se desfaz. Não aceita encantamentos (nem Inquebrável ou Remendo) e não pode ser consertado; para outro livro, faça o ritual do caldeirão de novo. No modo criativo não gasta.

- Custa **5 corações** de vida e dá **2 segundos de Náusea**.
- A invocação é recusada se mataria você, se o inventário estiver cheio ou se você **já tem uma arma**.
- Recarga de 5 s no livro.
- Só existe **uma arma por vez**.
- A arma **se dissipa em partículas de sangue** se for jogada no chão (tecla Q ou arrastando para fora do inventário), guardada em baú, barril, shulker, funil ou dispenser, ou se o jogador morrer. A lança ainda em voo some se você morrer ou sair do jogo, e é substituída se você invocar outra arma.

Todo o conteúdo fica na aba criativa **Veritates**.

## Como usar

1. Instale o [Forge 1.20.1](https://files.minecraftforge.net/) (47.3.0 ou superior).
2. Coloque o `.jar` do mod na pasta `mods`.
3. No modo criativo, abra a aba **Veritates**.

O `.jar` pronto fica na aba **Actions** do GitHub (artefato do último build).

## Compilar

Requer JDK 17 e Gradle 8.1.1.

```bash
gradle wrapper --gradle-version 8.1.1   # só na primeira vez (gera o ./gradlew)
./gradlew build                         # jar em build/libs/veritates-1.2.0.jar
./gradlew runClient                     # abre o Minecraft para testar
```

## Som personalizado

O som usa sons do próprio Minecraft (definidos em `assets/livro_das_sombras/sounds.json`).
Para usar um áudio seu:

1. Coloque o arquivo `.ogg` em `src/main/resources/assets/livro_das_sombras/sounds/blood_summon.ogg`.
2. Em `sounds.json`, troque a linha do `blood_summon` por:
   `{ "name": "livro_das_sombras:blood_summon" }`

Use apenas áudios que você tenha direito de distribuir.

## Adicionar novo conteúdo

**Nova arma invocável:**
1. Crie a classe em `item/` e registre em `registry/ModItems.java`.
2. Adicione-a em `summon/BloodWeapons.menuOrder()`: ela entra no menu do Livro do Sangue e já segue todas as regras (uma por vez, some ao largar, etc.).
3. Crie o modelo em `models/item/`, a textura em `textures/item/` e os nomes em `lang/`.

**Nova invocação do Livro do Tempo:** adicione-a em `summon/TimeSummon` e trate-a em `summon/TimeSummoning`. **Novo ritual menor:** adicione-o em `ritual/MinorRitual` com o caminho (`BLOOD` ou `TIME`) e trate-o em `MinorRituals.cast`.

**Novo item qualquer:** registre em `ModItems` e adicione em `displayItems(...)` de `registry/ModCreativeTabs.java`.

## Licença

Defina uma licença (por exemplo MIT) e atualize `mod_license` em `gradle.properties`.
