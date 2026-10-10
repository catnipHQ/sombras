# Veritates 1.0.5

Mod **Veritates** para Minecraft **Forge 1.20.1**. Adiciona o **Livro das Sombras**, que invoca a Espada de Sangue, e o **Livro do Sangue**, a versão melhorada dele, que deixa você escolher entre quatro armas de sangue ou invocar um **Ser de Sangue**.

## Livros

| Item | Descrição |
|------|-----------|
| Livro das Sombras | Botão direito invoca a Espada de Sangue. |
| Livro do Sangue | Botão direito abre um menu ritual para escolher: espada, lança, martelo, arco ou Ser de Sangue (clique ou teclas 1–5). Não substitui o Livro das Sombras, os dois existem. |

## Armas

| Arma | Dano | Particularidades |
|------|------|------------------|
| Espada de Sangue | 7 | Igual à espada de diamante (velocidade -2.4, 1561 de durabilidade). |
| Lança de Sangue | 6 | Segure o botão direito e solte para arremessar como um tridente. Ela volta sozinha para você depois de acertar ou de cravar em um bloco. O arremesso aplica Lentidão por 1,5 s e deixa o alvo sangrando pelo mesmo tempo (o golpe corpo a corpo não). |
| Martelo de Sangue | 10 | Lento (velocidade de ataque 0.6). Segure o botão direito para carregar (1 s) e solte: onda de choque que acerta todos em até 4 blocos com 5 de dano e empurra. Cada golpe custa 3 pontos de vida (1,5 coração). Recarga de 2 s. |
| Arco de Sangue | 7 | Não gasta flechas, mas cada flecha custa 1 coração. 7 de dano com o arco totalmente puxado, e o tiro totalmente puxado é crítico (bônus aleatório do Minecraft, até 11). A flecha não pode ser recolhida. |

## Ser de Sangue

Criatura invocada pelo Livro do Sangue (mesmo custo: 5 corações e Náusea).

- Como um zumbi, só que mais forte: 26 de vida (zumbi: 20) e 5 de dano (zumbi: 3). Não queima no sol.
- Dura **2 minutos** ou até a vida chegar a 0. O tempo restante aparece no inventário (tecla **E**) como um efeito de status.
- Segue e defende você como um lobo: ataca quem você atacar e quem atacar você.
- **Botão direito** nele ativa o **modo de guarda** (mensagem no chat): ele fica parado no lugar, mas continua revidando se você atacar ou for atacado, e depois volta ao posto. Clique de novo para ele voltar a seguir.
- Nunca ataca quem o invocou (nem os seus animais domesticados).
- **Shift + botão direito** nele (só quem o invocou) abre o menu ritual:
  1. **Prolongar:** custa 9,5 corações e o tempo restante volta para 10 minutos.
  2. **Desfazer:** ele morre na hora.
  3. **Sacrifício:** 3 corações saem da sua barra de vida (vida máxima) até ele morrer. Em troca ele ganha +5 de dano, +6 de armadura, +2 de resistência de armadura e resiste a empurrões. Uma vez por criatura.
- Só um Ser de Sangue por vez. Ele se dissipa se você morrer, ou se você sair do jogo ou trocar de dimensão por mais de 5 s.

## Regras de invocação (valem para os dois livros)

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
./gradlew build                         # jar em build/libs/veritates-1.0.5.jar
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

**Novo item qualquer:** registre em `ModItems` e adicione em `displayItems(...)` de `registry/ModCreativeTabs.java`.

## Licença

Defina uma licença (por exemplo MIT) e atualize `mod_license` em `gradle.properties`.
