import java.io.*;

/**
 * Created by Julien on 11/2/2015.
 */
public class DisqueLogique {
    private int diskSize;
    public DisquePhysique disque;
    private int tailleLimitName = 10; // taille limite pour un nom
    private int tailleAlloueNom = tailleLimitName+3; //le nom + le bloc de debut + la taille -> c'est la place pour retenir un fichier et ses attributs
    private int tailleLimitFile = 65535; // 2^16 -1 car les char sont codes sur 2 octets
    private int blocSize;
    private char delimiter = '_';

    /* Mon idee sera de stocker ma table d'occupation dans le bloc0, puis de stocker sous cette forme :
    nomfichier *caractere qui codera le bloc de d?but* *taille fichier*
    Au debut de chaque bloc de mon texte, il y a un caractere reserve pour le bloc suivant. Si c'est le dernier bloc dans lequel est
    stocke mon fichier, ce caractere contient 0
     */



    // constructeur : alloue un tableau dans le bloc 0. tableau d'occupation des blocs
    // le premier element (0) dit quelle est la prochaine case libre dans le bloc0
    public DisqueLogique(int taille){
        this.diskSize=taille;
        this.disque= new DisquePhysique(diskSize);
        this.blocSize=disque.getBlocSize();
        char[] tableauOcc= new char[blocSize];
        for (int i=0;i<diskSize+2;i++) {
            if (i==0){
                tableauOcc[i]=(char) (diskSize+2); // je retiens dans mon bloc0 le premier emplacement libre dans le bloc0
            } else if (i==1) {
                tableauOcc[i] = '1'; // le premier bloc est utilise (la FAT)
            } else if (i==(diskSize+1)) { // delimiteur de mon tableau
                tableauOcc[i] = delimiter;
            }else{ // le bloc n'est pas occupe
                tableauOcc[i]='0';
            }

        }
        disque.write(0, tableauOcc); // les autres sont initialement vides
        format(); // je formate le reste du DD
        //save();
    }




// formate les blocs de tout le disque dur du 1 ? la fin

    private void format() {
        char[] d = new char[blocSize];
        for(int i=1;i<diskSize;i++) {
            disque.write(i,d);
            d= new char[blocSize];
        }
        //save();
    }

    // fonction qui formate un bloc seulement de ma memoire
    private void format(int i){
        char[] d = new char[blocSize];
        disque.write(i,d);
        //save()
    }


    // permet la recuperation de panne, on RAZ tout ce qui etait en train d'etre execute
    // on peut le lancer pour enlever les 'w' et les 'a', car append et write sont des fonctions d'ecritures, et theoriquement,
    // il doit n'y avoir qu'un seul ecrivain en multithread (et par consequent aucun lecteur)

    public void scan_fix (){
        char[] bloc0 = disque.read(0);
        for (int i=1;i<(diskSize+1);i++){
            if (bloc0[i]=='w'){
                // quelqu'un etait en train d'ecrire, et ca plante. Tant pis on perd le travail
                format(i); //inutile mais on peut le faire
                bloc0[i]='0';
            }else if (bloc0[i]=='a'){ //ce bloc ?tait en cours d'append,
                // comme on voit le 'a', on n'a pas encore update la size du fichier.
                // le repasser a 1 ne fait que jeter le travail deja fait
                bloc0[i]='1';
            }else if (bloc0[i]=='r'){
                // nous etions en simple lecture. Et un lecteur ne lit jamais un fichier vide
                bloc0[i]='1';
            }
        }
        disque.write(0,bloc0);
        save();
    }



    // retourne les numero de n blocs vides, sinon 0, un char[] remplis de zeros
    private char[] bloc_libre(int n){
        char[] bloc0;
        bloc0=disque.read(0);
        char[] res= new char[n];
        int j=0; // curseur sur mon res.
        for (int i=1;i<diskSize+1;i++) { //je commence au numero 1, le 0 stocke autre chose (la prochaine place libre pour un nom)
            if (bloc0[i] == '0') {
                res[j++] = (char) (i-1); //il y a donc un decalage de 1
                bloc0[i]='w'; //quand j'appelle bloc_libre, c'est que je veux ecrire dedans
                if (j == n) { //alors je les ai tous
                    disque.write(0,bloc0); // je les bloque en ecriture
                    // et je l'ecris que si je sais que je vais ecrire dedans
                    return res;
                }
            }
        }
        // si j'arrive la c'est qu'il n'y a pas assez de blocs libres.
        char[] resf = new char[1];
        resf[0]='0';
        return resf;
    }


    // a partir d'une taille, donne le nombre de blocs n?cessaires
    private int nb_bloc_necessaires (int taille){
        return ((taille/(blocSize-1)))+1 ;
    }




    // extrait d'un char[] de debut a fin-1

    private char[] sub_char(char[] t,int debut, int fin){
        int len=fin-debut;
        char[] res = new char[len];
        for(int i=debut; i<fin; i++){
            res[i-debut]=t[i];
        }
        return res;
    }

    // fonction qui retourne l'indice qui indique la taille du fichier, s'il existe

    private int getIndiceSize(String nomfichier){
        char[] bloc0 = (disque.read(0));
        String test;
        for (int i=diskSize+2;i<((int) bloc0[0]) ;i+=tailleAlloueNom){ // on commence a regarder apres le tableau d'occupation
            test=new String((sub_char(bloc0,i,i+nomfichier.length())));
            if (nomfichier.equalsIgnoreCase(test)){
                return (i+tailleLimitName+1); //je retourne l'indice
            }
        }
        return 0; //jamais atteint

    }


    // retourne si un fichier existe, le bloc utilise et sa taille. Sinon 0,0
    // place en public car peut etre utile par l'utilisateur
    public int[] existe_fichier (String nomfichier){
        char[] bloc0 = (disque.read(0));
        String test;
        int[] res=new int[2];
        for (int i=diskSize+2;i<((int) bloc0[0]) ;i+=tailleAlloueNom){ // on commence a regarder apres le tableau d'occupation
            // et comme on a normalise les places, on peut y aller par grands pas de "tailleAlloueNom"
            test=new String((sub_char(bloc0,i,i+nomfichier.length()))); //j'extrais proprement sans espace
            if (nomfichier.equalsIgnoreCase(test)){
                res[0]=(int) bloc0[i+tailleLimitName]; // le premier bloc
                res[1]=(int) bloc0[i+tailleLimitName+1]; // la taille
                return res;
            }
        }
        res[0]=0;
        res[1]=0;
        return res; //juste pour avoir un retour d'erreur
    }


    // donne la taille du fichier s'il existe
    // place en public car cela peut etre utile pour l'utilisateur
    public int getFileSize(String nomfichier){
        int[] bloc=existe_fichier(nomfichier);
        if (bloc[0]==0){
            System.out.println("Le fichier "+ nomfichier +" n'existe pas");
            return 0;
        }
        return bloc[1];
    }





    //fonction qui va donner la liste des blocs occupes par un fichier, s'il existe

    private int[] getBlocFile(String nomfichier){
        int[] bloc=existe_fichier(nomfichier);
        if (bloc[0]==0){ //s'il existe pas
            System.out.println("Le fichier "+ nomfichier +" n'existe pas");
            int[] res={0};
            return res;
        }
        int N= nb_bloc_necessaires(getFileSize(nomfichier)); // calcule le nb de blocs necessaires
        int[] res= new int[N];
        res[0]=bloc[0]; // le premier bloc est connu
        char[] bloc_suite = disque.read(bloc[0]);
        for(int i=1;i<N;i++){
            // je parcours la liste chainee
            res[i]=(int) bloc_suite[0];
            bloc_suite = disque.read(res[i]);
        }
        return res;
    }


    // fonction qui prend tocpy ? partir de debut, et l'?crit dans aim a partir de j
    private char[] ecrit_bloc(char[] aim, char[] tocpy, int j, int debut){
        int k;
        int i=debut;
        for (k=j;k<blocSize;k++){
            aim[k]=tocpy[i];
            i++;
            if(i>=tocpy.length){
                return aim;
            }
        }
        return aim;
    }



    // fonction lecture, en public.

    public char[] fread(String nomfichier){
        char[] bloc1 = new char[blocSize];
        int[] bloc = existe_fichier(nomfichier); //on regarde si le fichier existe
        if (bloc[0]==0){
            System.out.println("Ce fichier n'existe pas ! ");
            return bloc1;
        }
        String res;
        int taille = bloc[1];
        int nbblocs= nb_bloc_necessaires(taille);
        //cas ou ca tient sur un seul bloc
        if (nbblocs==1){
            // je ne recupere que ce qu'il m'interesse dans le bloc (j'enleve les espaces)
            res= new String(sub_char(disque.read(bloc[0]),1,taille+1));
            return res.toCharArray();
        }

        // cas ou ca ne tient pas sur un seul bloc
        // on recupere le premier bloc
        res = new String(sub_char(disque.read(bloc[0]),1,blocSize));
        int[] blocs= getBlocFile(nomfichier); //liste des blocs occupes par le fichier
        for (int i=1;i<nbblocs;i++){ // je vais regarder tous les blocs occuppes par ce fichier
            if (i==nbblocs-1){ // si c'est le dernier bloc,
                //je lis que jusqu'a la fin de mon texte et pas tout le bloc
                res+= new String (sub_char((disque.read( blocs[i])),1, (taille+1)%blocSize));
            }else { // Sinon,  je lis tout le bloc.
                res += new String(sub_char(disque.read(blocs[i]), 1, blocSize));
            }
        }
        return res.toCharArray();
    }


    // fonction d'ecriture, en public.
    public void fwrite (String nomfichier, char[] data) {
        if (nomfichier.length()>tailleLimitName) { // je verifie si la taille n'est pa strop grande
            System.out.format("Le nom du fichier est trop grand. Taille limite : %d \n",tailleLimitName);
            return ;
        }
        int[] bloc= existe_fichier(nomfichier);
         if (bloc[0]!=0){ // je verifie si le fichier n'existe pas
             System.out.println("Le nom du fichier " + nomfichier+ " est deja utilise. Veuillez le renommer ");
             return ;
         }

        if (data.length > tailleLimitFile){
            // je verifie si le fichier ne depasse pas une trop grande taille
            System.out.format("La taille du fichier est trop grand. Taille limite : %d \n",tailleLimitFile);
            return ;
        }
        int nb_blocs=nb_bloc_necessaires(data.length);
        char[] bvide=bloc_libre(nb_blocs);
        // ICI : je declare ces blocs en w si on est dans le bon cas
        if ((int) bvide[0]==0){
            // je regarde s'il reste assez de blocs disponibles
            System.out.println("Il n'y a plus de place disponible sur le disque dur ! ");
            return ;
        }

        char[] bloc0=disque.read(0);
        int prochaine_place_FAT = (int) bloc0[0];
        if (blocSize -prochaine_place_FAT<tailleAlloueNom){
            System.out.println("Il n'y a plus de place disponible sur la FAT ! ");
            scan_fix(); // je passe tous les 'w' en '0'
            return ;
        }
        // on attaque la boucle for qui me permet d'ecrire data dans la memoire
        for (int i=0;i<nb_blocs;i++){
            char[] bloc_encours=new char[blocSize];
            if (i==nb_blocs-1){
                bloc_encours[0]=(char) 0; // si je suis a la fin, je mets conventionnellement a 0 (fin de la liste chainee)
            }else{
                bloc_encours[0] = bvide[i+1]; // je note le bloc prochain utilise (liste chainee)
            }
            if(i==0){
                bloc_encours=ecrit_bloc(bloc_encours, data, 1,0); // j'ecris le match que je peux
            }else{
                bloc_encours=ecrit_bloc(bloc_encours, data, 1,(i*blocSize)+2);
                // double decalage, dus a la retenue d'un pour le stockage de la liste chainee.
            }
            // j'ecris tout pour ce bloc
            disque.write((int) bvide[i], bloc_encours);
        }
        // Derniere partie : on update la FAT
        // on aurait pu fusionner les deux boucles, mais je prefere etre clair dans mon code et ne pas tout melanger
        // de plus, si on veut utiliser la FAT comme un s?maphore en moins bien, il faut le finir a la fin
        int a_bloquer;
        for (int i=0;i<nb_blocs;i++){
            a_bloquer= ((int) bvide[i])+1;
            bloc0[a_bloquer]='1'; //j'indique les blocs occupes, cela supprime les 'w'
        }
        int prochain;
        char[] nom= nomfichier.toCharArray();
        int j=0;
        for (prochain=((int)bloc0[0]);prochain<(nom.length+((int)bloc0[0]));prochain++){
            bloc0[prochain]=nom[j]; // un peu complique pour update mon premier nombre
            j++;
        }
        prochain=(((int)bloc0[0]))+tailleLimitName;
        bloc0[prochain]=bvide[0]; // je dis dans quel bloc commence mon fichier
        bloc0[prochain+1]=(char) data.length; // je stocke la taille
        bloc0[prochain+2]='_'; // je mets un delimiteur
        bloc0[0]=(char) (prochain+3); // ici, j'update reellement mon premier chiffre dans la FAT.
        disque.write(0,bloc0); // les 'w' sont vires
        save();
    }


    // fonction append, public.

    public void fappend(String nomfichier, char[] data) {

        int[] bloc = existe_fichier(nomfichier); // routine
        if (bloc[0] == 0) {
            System.out.println("Le fichier " + nomfichier + " n'existe pas. Essayez avec la methode write");
            return;
        }
        // deja on va recuperer tous les blocs qui contiennent le fichier
        int taille = getFileSize(nomfichier);
        int nb_bloc_avant = nb_bloc_necessaires(taille);
        int[] blocs_avant = getBlocFile(nomfichier); // les blocs
        int taille_dispo_last_bloc = blocSize - (taille % blocSize); // on regarde la taille disponible sans allouer d'autre bloc
        char[] bloc0=disque.read(0);
        bloc0[blocs_avant[nb_bloc_avant-1]]='a'; //le seul bloc qui va etre en append
        disque.write(0,bloc0);
        if (data.length < taille_dispo_last_bloc) { // si pas besoin d'allouer d'autres blocs
            char[] dernierbloc = disque.read(blocs_avant[nb_bloc_avant-1]);
            dernierbloc = ecrit_bloc(dernierbloc, data, (taille % blocSize) - 1, 0); // on l'ecrit
            disque.write(blocs_avant[nb_bloc_avant - 1], dernierbloc);
            // je decide d'update la taille en dernier, comme ca si cela plante entre les deux, la non actualisation de la taille
            // fera comme si le fichier n'avait jamais ?t? ecrit.
            bloc0 = disque.read(0);
            bloc0[getIndiceSize(nomfichier)] = (char) (taille + data.length);
            bloc0[blocs_avant[nb_bloc_avant-1]]='1'; //je rends la main
            disque.write(0, bloc0);
            save();
            return;
        } else { //je dois allouer d'autres blocs
            // taille a repartir sur d'autres blocs
            int taille_a_allouer = data.length - taille_dispo_last_bloc;
            int nb_blocs_apres = nb_bloc_necessaires(taille_a_allouer);
            char[] bvide = bloc_libre(nb_blocs_apres);
            // Ces blocs sont en w

            if ((int) bvide[0] == 0) {
                System.out.println("Il n'y a pas assez de place dans le disque dur !");
                return;
            }
            // ecriture dans la memoire, similaire a write.
            for (int i = 0;i<(nb_blocs_apres+1); i++) {
                char[] bloc_encours;

                if (i == 0) {
                    // je complete d'abord le bloc entame par le fichier.
                    char[] dernierbloc = disque.read(blocs_avant[nb_bloc_avant-1]);
                    bloc_encours = ecrit_bloc(dernierbloc, data, (taille-1) % blocSize, 0);
                    bloc_encours[0]=bvide[0];
                    disque.write(blocs_avant[nb_bloc_avant-1], bloc_encours);
                } else if (i<nb_blocs_apres){
                    // puis je reserve d'autres cases memoires
                    bloc_encours = new char[blocSize];
                    bloc_encours[0]=bvide[i];
                    bloc_encours = ecrit_bloc(bloc_encours, data, 1, taille_dispo_last_bloc + (i - 1) * blocSize);
                    disque.write((int) bvide[i-1], bloc_encours);
                } else if (i==nb_blocs_apres){
                    bloc_encours = new char[blocSize];
                    bloc_encours[0]=(char)0; //c'est le dernier bloc, je le pointe vers 0
                    bloc_encours = ecrit_bloc(bloc_encours, data, 1, taille_dispo_last_bloc + (i - 1) * blocSize);
                    disque.write((int) bvide[i-1], bloc_encours);
                }
            }
            // je decide d'update la taille en dernier, comme ca si cela plante entre les deux, la non actualisation de la taille
            // fera comme si le fichier n'avait jamais ?t? ecrit.
            bloc0 = disque.read(0);
            bloc0[getIndiceSize(nomfichier)] = (char) (taille + data.length);
            // puis je mets a jour la table d'allocation
            for (int i=0;i<nb_blocs_apres;i++){
                bloc0[(int) bvide[i]+1]='1'; //decalage d'un indice avec la retenue // les 'w' se barrent
            }
            bloc0[blocs_avant[nb_bloc_avant-1]]='1'; //je passe le append en '1' comme prevu.
            disque.write(0, bloc0);
            save();
        }
    }





    // une fonction qui delete les fichiers, s'ils existent

    public void fdelete(String nomfichier){
        int[] bloc = existe_fichier(nomfichier); // routine
        if (bloc[0] == 0) {
            System.out.println("Le fichier " + nomfichier + " n'existe pas.");
            return;
        }
        int[] toutbloc=getBlocFile(nomfichier);
        char[] bloc0 = disque.read(0);
        // desallocation dans la FAT
        for(int i=0;i<(toutbloc.length);i++){
            bloc0[toutbloc[i]+1]='0';
        }
        // suppression du nom dans la FAT
        int indice = getIndiceSize(nomfichier)-tailleAlloueNom+1;
        char[] res = new char[blocSize];
        int offset=0;
        for (int i=0; i<blocSize;i++){
            if (i==indice){
                // je decale tout
                i+=(tailleAlloueNom);
                offset+=(tailleAlloueNom);
            }
            res[i-offset]=bloc0[i];
        }
        // on n'oublie pas de decaler aussi le pointeur d'ecriture d'attributs a sa place
        res[0]= (char) ((int)res[0]-tailleAlloueNom) ;
        disque.write(0,res);
        // on fait le format a la fin, car ce n'est pas primordial -> on peut reecrire dessus sans probleme
        // etant donne que la FAT a oublie la presence de fichier, mais on format pour que ce soit plus joli
        for(int i=0;i<(toutbloc.length);i++){
            format(toutbloc[i]);
        }
        save(); // on sauvegarde
    }


    // petit print pour afficher l'etat du disque dur
    public void print (){
        for (int i=0;i<diskSize;i++){
            System.out.print ("Bloc : "+ i +" ");
            System.out.println(disque.read(i));
        }
        System.out.println();
    }

    // petite fonction sauvegarde pour stocker le DD sur un .txt Attention il y a des problemes de caracteres non reconnus
    public void save(){
        DataOutputStream dos;
        try {
            dos = new DataOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(
                                    new File("backup.txt"))));

        for (int i=0;i<diskSize;i++){
            // j'ecris chaque ligne
            dos.writeBytes("Bloc " + i+ " : "+ new String(disque.read(i))+"\n");
        }
            dos.close();

            // gestion des exceptions.
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }


    // fonction load publique
    // il faut que le backup soit issu d'un DD de m?me taille sinon ca plante ! On va le supposer

    public void load(){
        DataInputStream dis;
        try {
            dis = new DataInputStream(
                    new BufferedInputStream(
                            new FileInputStream(
                                    new File("backup.txt"))));

            String line=dis.readLine();
            char[][] newdata= new char[diskSize][blocSize];
            int i=0;

            while (line != null) {
                newdata[i]=sub_char(line.toCharArray(),9,line.length());
                line=dis.readLine();
                i++;
            }
            for(i=1;i<diskSize;i++){
                // on ecrit toute la memoire
                disque.write(i,newdata[i]);
            }
            // et seulement a la fin on ecrit la table d'occupation pour eviter les bugs de panne
            disque.write(0,newdata[0]);

        } catch (FileNotFoundException e) {
            System.out.println("Il n'y a rien a charger ! ");
            e.printStackTrace();
        }  catch (IOException e) {
            e.printStackTrace();

        }

    }



    public void affiche_contenu(){

        char[] bloc0= disque.read(0);
        String nom;
        char[] contenu;
        int taille;
        int nbfiles=0;
        System.out.println("Contenu du disque dur");
        for (int i=diskSize+2;i<blocSize;i+=tailleAlloueNom){
            if(bloc0[i+tailleAlloueNom-1]==delimiter){
                nbfiles++;
                nom = new String (sub_char(bloc0,i,i+tailleLimitName));
                contenu = fread(nom);
                taille=getFileSize(nom);
                System.out.println("Nom de fichier : " + nom);
                System.out.println("Sa taille : "+ taille +" caracteres");
                System.out.print("Son contenu : ");
                System.out.println(contenu);
                System.out.println();
            }
        }
        System.out.println("Soit "+nbfiles+" fichiers");
        System.out.println();
    }






}
