# GuessQuest

## 📚 Índice

<div align="center">

<table>
  <tr>
    <td><a href="#visão-geral">🔹 Visão Geral</a></td>
    <td><a href="#funcionalidades">🎮 Funcionalidades</a></td>
    <td><a href="#tecnologias">⚙️ Tecnologias</a></td>
  </tr>
  <tr>
    <td><a href="#apis">🤖 APIs </a></td>
    <td><a href="#estrutura-do-projeto">🏗️ Estrutura</a></td>
    <td><a href="#instalação">💻 Instalação</a></td>
  </tr>
  <tr>
    <td><a href="#execução">🚀 Execução</a></td>
    <td><a href="#autores">👥 Autores</a></td>
    <td><a href="#licença">📄 Licença</a></td>
  </tr>
</table>

</div>

---

## Visão Geral

**GuessQuest** é um jogo educacional em JavaFX onde o usuário deve
adivinhar palavras com base em imagens, sons e transições animadas. O
projeto inclui gerenciamento de ranking, sons, telas primária/secundária
e empacotamento via `jpackage`.

---

## Funcionalidades

-   Sistema de jogo com estados (GameState);
-   Interface JavaFX com múltiplas telas (`primary.fxml`, `secondary.fxml`, `leaderboard.fxml`);
-   Sons integrados via `AudioClip`;
-   Rankings persistidos em `user.home/guessquest_leaderboard.txt`;
-   Animações com `FadeTransition`;
-   Empacotamento em executável desktop.

---

## Tecnologias

-   **Java 17+**;
-   **JavaFX**;
-   **Maven**;
-   **CSS**;
-   **JPackage (JDK)**;
-   **Biblioteca http**.

---

## APIs

-   **RAWG**: Usado para jogo funcionar;
-   **Github API**: Usado para mostrar o avatar dos desenvolvedores; 
-   **The Cat API**: Usado no easter egg escrevendo "GATO";
-   **Random-d.uk**: Usado no easter egg escrevendo "PATO";
-   **Dog API**: Usado no easter egg escrevendo "DOG".

---

## Estrutura do Projeto

    GuessQuest/
    ├── src/main/
    │   ├── java/
    │   │   ├── module-info.java
    │   │   └── br/edu/unijui/piu/guessquest/
    │   │       ├── App.java
    │   │       ├── Launcher.java
    │   │       ├── GameState.java
    │   │       ├── SoundManager.java
    │   │       ├── PrimaryController.java
    │   │       ├── SecondaryController.java
    │   │       ├── LeaderboardController.java
    │   │       └── LeaderboardManager.java
    │   │    
    │   └── resources/
    │       ├── fonts/
    │       └── br/edu/unijui/piu/guessquest/
    │           ├── audio/
    │           ├── primary.fxml
    │           ├── secondary.fxml
    │           ├── leaderboard.fxml
    │           ├── styles.css
    │           └── Arcade.jpg
    │
    ├── pom.xml
    └── README.md

---

## Instalação

``` bash
# Clone
git clone https://github.com/LucasSckenal/GuessQuest.git
cd GuessQuest

# Executar via Maven
mvn clean javafx:run
```

---

## Execução

Após rodar:

``` bash
mvn clean javafx:jlink
```

Use **jpackage**:

``` bash
jpackage   --name GuessQuest   --input "./target"   --main-jar "GuessQuest-1.0-SNAPSHOT.jar"   --main-class "br.edu.unijui.piu.guessquest.Launcher"   --type app-image   --dest "dist"
```

---

## Contribuição

Seja bem-vindo a colaborar!  

1. Faça um fork deste repositório  
2. Crie uma branch com sua feature ou correção: `git checkout -b minha-feature`  
3. Faça commits das suas alterações: `git commit -m "Descrição da feature"`  
4. Envie para seu fork: `git push origin minha-feature`  
5. Abra um Pull Request explicando a mudança  

Por favor siga o padrão de código, mantenha testes atualizados, etc.

---

## Autores

| Nome              | Links | E-Mail |
| ----------------- | ---------------------- | ---------------------- |
| Henrique Luan F.  | [LinkedIn](https://www.linkedin.com/in/henrique-luan-fritz-70412635a/)        | [Henrique.fritz@sou.unijui.edu.br](mailto:Henrique.fritz@sou.unijui.edu.br) |
| Luan Vitor C. D. | [LinkedIn](https://www.linkedin.com/in/luan-vitor-casali-dallabrida-20a60a342/)        | [luanvitorcd@gmail.com](mailto:luanvitorcd@gmail.com) |
| Lucas P. Sckenal   | [LinkedIn](https://www.linkedin.com/in/lucassckenal/)        | [lucaspsckenal@gmail.com](mailto:lucaspsckenal@gmail.com) |

---

## Licença

Este projeto está licenciado sob os termos da licença [Apache](./LICENSE).
