# ID

Mod **ID** para Minecraft **Forge 1.20.1**, que adiciona o **Livro das Sombras**, um livro preto capaz de invocar a **Espada de Sangue**.

## Conteúdo

| Item | Descrição |
|------|-----------|
| Livro das Sombras | Botão direito invoca a Espada de Sangue (uma por vez, recarga de 5 s), com som de magia de sangue e partículas vermelhas. |
| Espada de Sangue | Mesmos atributos da espada de diamante: 7 de dano, velocidade de ataque -2.4, 1561 de durabilidade. |

Todo o conteúdo fica na aba própria **ID** do modo criativo.

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

1. Crie a classe do item em `item/` e registre em `registry/ModItems.java`.
2. Adicione-o em `displayItems(...)` de `registry/ModCreativeTabs.java` (a aba do mod).
3. Crie o modelo em `models/item/`, a textura em `textures/item/` e os nomes em `lang/`.

## Licença

Defina uma licença (por exemplo MIT) e atualize `mod_license` em `gradle.properties`.
