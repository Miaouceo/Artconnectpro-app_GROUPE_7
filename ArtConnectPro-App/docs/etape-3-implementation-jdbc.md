# Etape 3 - Implementation JDBC et integration applicative

## 1. Objectif de l'etape

L'objectif de cette etape est de faire evoluer l'application ArtConnect Pro d'un fonctionnement purement en memoire vers un fonctionnement appuye sur une base de donnees relationnelle MySQL.

Cette etape porte sur trois aspects principaux :

- etablir une connexion JDBC vers la base `artconnect_pro`
- implementer les acces aux donnees pour certaines entites du domaine
- brancher l'application pour qu'elle puisse utiliser les donnees de la base dans l'interface

Dans le squelette fourni, les classes metier Java ne contiennent pas d'identifiants techniques. Le travail de cette etape consiste donc a reconstruire les objets metier a partir des cles primaires et cles etrangeres presentes uniquement dans le schema relationnel.

## 2. Perimetre implemente

Le perimetre retenu pour cette etape couvre :

- la configuration de la connexion a la base
- l'implementation JDBC de `ArtistDao`
- l'implementation JDBC de `ArtworkDao`
- l'ajout de services JDBC pour les artistes et les oeuvres
- la mise a jour du `ServiceProvider` pour utiliser JDBC lorsque la base est disponible

Les autres domaines de l'application, comme les galeries, ateliers et membres, restent sur des services en memoire afin de conserver une application fonctionnelle sans devoir implementer l'ensemble des DAO en une seule iteration.

## 3. Architecture retenue

L'architecture par couches du projet est conservee :

- couche presentation : controleurs JavaFX et vues FXML
- couche service : orchestration des besoins fonctionnels de l'interface
- couche DAO : contrats d'acces aux donnees
- couche persistence : implementations JDBC
- couche base de donnees : MySQL

Le flux d'execution est le suivant :

1. un controleur UI demande des donnees a un service
2. le service appelle un DAO
3. le DAO execute une requete SQL via JDBC
4. le resultat SQL est transforme en objets Java
5. les objets sont renvoyes a l'interface

## 4. Configuration de la connexion JDBC

Deux classes ont ete mises en place pour centraliser l'acces a la base :

### `DatabaseConfig`

Cette classe contient les parametres de connexion :

- URL JDBC
- utilisateur
- mot de passe

Les valeurs peuvent etre surchargees via des variables d'environnement :

- `ARTCONNECT_DB_URL`
- `ARTCONNECT_DB_USER`
- `ARTCONNECT_DB_PASSWORD`

Ce choix evite de figer les identifiants directement dans le code source.

### `ConnectionManager`

La classe `ConnectionManager` encapsule la creation d'une connexion JDBC via `DriverManager.getConnection(...)`.

Elle permet :

- d'eviter la duplication du code de connexion
- de centraliser la logique technique
- de simplifier les DAO

## 5. Implementation de `JdbcArtistDao`

La classe `JdbcArtistDao` implemente l'interface `ArtistDao`.

Les operations prises en charge sont :

- `findAll()`
- `save(Artist artist)`
- `update(Artist artist)`
- `delete(String artistName)`
- `findByCity(String city)`

### Chargement des artistes

Le chargement des artistes repose sur une requete avec jointures entre :

- `artist`
- `artist_discipline`
- `discipline`

Cela permet de reconstituer :

- les informations propres a l'artiste
- la liste de ses disciplines

Une structure intermediaire de type `Map<Integer, Artist>` est utilisee pour eviter de recreer plusieurs fois le meme artiste lorsque plusieurs lignes SQL correspondent au meme objet a cause des jointures.

### Insertion et mise a jour

L'insertion d'un artiste est effectuee dans la table `artist`, puis les disciplines sont rattachees dans `artist_discipline`.

La mise a jour :

- modifie les attributs de l'artiste
- supprime les anciennes associations dans `artist_discipline`
- recree les associations a partir de la liste courante des disciplines

### Gestion des disciplines

Avant de lier une discipline a un artiste, il faut verifier qu'elle existe dans la table `discipline`.

La methode `ensureDiscipline(...)` :

- recherche d'abord la discipline par nom
- l'insere si elle n'existe pas
- recupere ensuite son identifiant

Cette logique permet de garder une base coherente et normalisee.

## 6. Implementation de `JdbcArtworkDao`

La classe `JdbcArtworkDao` implemente l'interface `ArtworkDao`.

Les operations prises en charge sont :

- `findAll()`
- `save(Artwork artwork)`
- `update(Artwork artwork)`
- `delete(String title)`
- `findByArtistName(String artistName)`

### Chargement des oeuvres

Le chargement des oeuvres s'appuie sur des jointures entre :

- `artwork`
- `artist`
- `artist_discipline`
- `discipline`
- `artwork_tag_map`
- `artwork_tag`

L'objectif est de reconstruire un graphe objet plus riche :

- une oeuvre
- son artiste
- les disciplines de cet artiste
- les tags de l'oeuvre

Deux structures de memorisation sont utilisees :

- `Map<Integer, Artist>` pour mutualiser les artistes deja reconstruits
- `Map<Integer, Artwork>` pour mutualiser les oeuvres deja reconstruites

Ce mecanisme evite les doublons et permet de reconstituer correctement les relations.

### Insertion et mise a jour

Lors d'un `save` :

- l'identifiant de l'artiste est retrouve dans la base
- l'oeuvre est inserree dans `artwork`
- les tags sont rattaches via `artwork_tag_map`

Lors d'un `update` :

- les colonnes de l'oeuvre sont mises a jour
- les anciens liens de tags sont supprimes
- les nouveaux liens sont recrees

### Gestion des tags

Comme pour les disciplines, la methode `ensureTag(...)` :

- recherche un tag existant
- l'insere si besoin
- renvoie l'identifiant associe

Cette approche garantit que la table `artwork_tag` joue bien le role de referentiel.

## 7. Ajout des services JDBC

Deux services specifiques ont ete ajoutes :

- `JdbcArtistService`
- `JdbcArtworkService`

Leur role est d'exposer a l'interface les memes contrats que les services en memoire, mais en s'appuyant sur les DAO JDBC.

Ils permettent notamment :

- de recuperer tous les artistes et toutes les oeuvres
- de rechercher un artiste ou une oeuvre par nom
- de filtrer les oeuvres par artiste
- de creer, modifier et supprimer des enregistrements

Cela permet de conserver une separation claire entre :

- la logique metier simple
- l'acces technique a la base

## 8. Integration dans `ServiceProvider`

La classe `ServiceProvider` a ete adaptee pour choisir automatiquement le mode de fonctionnement.

Le comportement retenu est le suivant :

- si la connexion JDBC fonctionne et que les tables essentielles existent, l'application utilise les services JDBC
- sinon, l'application repasse sur les services en memoire

Ce choix presente deux avantages :

- il permet de tester l'application meme si MySQL n'est pas encore pret
- il evite un plantage complet au demarrage

Le systeme reste donc progressif et robuste.

## 9. Difficultes techniques rencontrees

Plusieurs difficultes sont apparues pendant cette etape.

### Absence d'identifiants dans les modeles Java

Les objets `Artist` et `Artwork` ne contiennent pas de champ `id`.

Il a donc fallu :

- manipuler les identifiants uniquement dans la couche DAO
- reconstituer les relations par jointure SQL
- associer les lignes SQL aux bons objets Java via des maps intermediaires

### Reconstruction du graphe objet

Une simple requete SQL avec jointures produit plusieurs lignes pour une meme oeuvre ou un meme artiste.

Sans traitement supplementaire, cela creerait :

- des doublons d'objets
- des listes incorrectes
- des relations incoherentes

L'utilisation de maps indexees par identifiant a permis de corriger ce probleme.

### Gestion des tables d'association

Les relations plusieurs-a-plusieurs comme :

- artiste <-> discipline
- oeuvre <-> tag

imposent une logique supplementaire :

- verification de l'existence des valeurs de reference
- insertion conditionnelle
- recreation des liens lors des mises a jour

## 10. Tests et verification

La verification effectuee a ce stade repose sur :

- la compilation des classes modifiees avec `javac`
- la coherence des requetes SQL avec le schema defini dans `sql/01_schema.sql`
- la compatibilite avec les interfaces `ArtistDao`, `ArtworkDao`, `ArtistService` et `ArtworkService`

Limite actuelle :

- Maven n'etait pas disponible dans l'environnement de travail au moment de la verification
- un test complet de l'application avec une base MySQL peuplee reste a realiser localement