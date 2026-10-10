# ID 1.0.3

Mod **ID** para Minecraft **Forge 1.20.1**. Adiciona o **Livro das Sombras**, que invoca a Espada de Sangue, e o **Livro do Sangue**, a versão melhorada dele, que deixa você escolher entre quatro armas de sangue.

## Livros

| Item | Descrição |
|------|-----------|
| Livro das Sombras | Botão direito invoca a Espada de Sangue. |
| Livro do Sangue | Botão direito abre um menu para escolher a arma: espada, lança, martelo ou arco. Não substitui o Livro das Sombras, os dois existem. |

## Armas

| Arma | Dano | Particularidades |
|------|------|------------------|
| Espada de Sangue | 7 | Igual à espada de diamante (velocidade -2.4, 1561 de durabilidade). |
| Lança de Sangue | 6 | Segure o botão direito e solte para arremessar como um tridente. Ela volta sozinha para você depois de acertar ou de cravar em um bloco. |
| Martelo de Sangue | 10 | Lento (velocidade de ataque 0.6). Botão direito: onda de choque que acerta todos em até 4 blocos com 5 de dano e empurra. Recarga de 2 s. |
| Arco de Sangue | até 6 | Não gasta flechas. Dano máximo com o arco totalmente puxado, sem acerto crítico. A flecha não pode ser recolhida. |

## Regras de invocação (valem para os dois livros)

- Custa **5 corações** de vida e dá **2 segundos de Náusea**.
- A invocação é recusada se mataria você, se o inventário estiver cheio ou se você **já tem uma arma**.
- Recarga de 5 s no livro.
- Só existe **uma arma por vez**.
- A arma **se dissipa em partículas de sangue** se for jogada no chão (tecla Q ou arrastando para fora do inventário), guardada em baú, barril, shulker, funil ou dispenser, ou se o jogador morrer. A lança ainda em voo some se você morrer ou sair do jogo, e é substituída se você invocar outra arma.

Todo o conteúdo fica na aba criativa **ID**.

## Como usar

1. Instale o [Forge 1.20.1](https://files.minecraftforge.net/) (47.3.0 ou superior).
2. Coloque o `.jar` do mod na pasta `mods`.
3. No modo criativo, abra a aba **ID**.

O `.jar` pronto fica na aba **Actions** do GitHub (artefato do último build).

## Compilar

Requer JDK 17 e Gradle 8.1.1.

```bash
gradle wrapper --gradle-version 8.1.1   # só na primeira vez (gera o ./gradlew)
./gradlew build                         # jar em build/libs/
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
