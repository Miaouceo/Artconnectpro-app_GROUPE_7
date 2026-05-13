# Etape 4 - Integration de la base dans l'application Java ArtConnect

## 1. DAO

Pour cette etape, les DAO JDBC ont ete mis en place pour connecter l'application a une vraie base de donnees MySQL.

Les classes suivantes ont ete implementees :

- `JdbcArtistDao`
- `JdbcArtworkDao`
- `JdbcGalleryDao`
- `JdbcWorkshopDao`
- `JdbcCommunityMemberDao`

Elles permettent de lire les donnees de la base et, pour les entites principales deja prevues, de gerer les operations utiles avec `PreparedStatement` et une fermeture correcte des ressources.

## 2. Services

La couche service a ete adaptee pour utiliser les DAO JDBC au lieu des services en memoire lorsque la base est disponible.

Les services suivants ont ete ajoutes :

- `JdbcArtistService`
- `JdbcArtworkService`
- `JdbcGalleryService`
- `JdbcWorkshopService`
- `JdbcCommunityService`

Ces services conservent l'architecture du projet et servent d'intermediaire entre l'interface et la couche DAO.

## 3. Connexion a la base

La connexion JDBC a ete centralisee avec :

- `DatabaseConfig`
- `ConnectionManager`

`DatabaseConfig` contient l'URL, l'utilisateur et le mot de passe de la base.  
`ConnectionManager` ouvre les connexions JDBC utilisees par les DAO.

Cette organisation permet d'avoir une configuration claire et reutilisable dans toute l'application.

## 4. Adaptation de l'interface

L'interface n'a pas ete refaite en profondeur, mais elle utilise maintenant les services connectes a la base lorsque celle-ci est disponible.

Cette integration passe par `ServiceProvider`, qui :

- verifie si la connexion JDBC fonctionne
- verifie si les tables principales existent
- utilise les services JDBC si la base est prete
- conserve un mode in-memory sinon

Ainsi, les ecrans `Artists`, `Artworks`, `Galleries`, `Exhibitions`, `Workshops`, `Community` et `Discover` peuvent afficher des donnees reelles provenant de la base.


## Recapitulatif rapide du code

Le code a ete modifie pour connecter l'application a MySQL.  
La connexion JDBC a ete ajoutee, les DAO `Artist`, `Artwork`, `Gallery`, `Workshop` et `CommunityMember` ont ete implementes, des services JDBC ont ete crees, puis `ServiceProvider` a ete adapte pour utiliser la base quand elle est disponible et revenir au mode en memoire sinon.
