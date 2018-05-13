/**
 * Created by Julien on 11/7/2015.
 */



public class Main{
    public static void main (String[] args){
        DisqueLogique logiciel= new DisqueLogique(10);
        logiciel.print();
        // test ecriture basique

        String test= "toto";
        char[] data={'H','e','l','l','o'};
        logiciel.fwrite(test,data);
        logiciel.print();

        // test taille

        int taille= logiciel.getFileSize("toto");
        System.out.println("Le fichier toto a pour taille "+ taille);

        // test ecriture sur plusieurs blocs

        test="nana";
        String str="coucoucoucoucoucoucoucoucoucoucoucoucoucoucoucoucoucoucoucoucoucou!!";
        char[] data2 = str.toCharArray();
        logiciel.fwrite(test,data2);
        logiciel.print();

        logiciel.affiche_contenu();

        // test du append sans reallocation OK



        test="nana";
        char[] data4 = {'d','e','p','i','n','f','o'};
        logiciel.fappend(test,data4);
        logiciel.print();


        logiciel.affiche_contenu();



        // test du append avec reallocation de bloc

        str="abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabc!!";
        data2 = str.toCharArray();

        logiciel.fappend(test,data2);
        logiciel.print();

        logiciel.affiche_contenu();




        // experimentation de la fonction fdelete
        logiciel.fdelete("toto");
        logiciel.print();
        // ca marche !
        logiciel.affiche_contenu();


        // reecriture après suppression
        String fin = "Conclusion";
        char[] findata = "Depinfo is best dep' ! ".toCharArray();
        logiciel.fwrite(fin,findata);
        logiciel.print();
        // il reecrit bien par dessus le bloc libere

        System.out.println(logiciel.fread("Conclusion"));
        logiciel.save();

        //je supprime tout pour tester mon load
        logiciel = new DisqueLogique(10);
        logiciel.print();
        logiciel.affiche_contenu();
        // le DD est bien  vide

        logiciel.load();
        System.out.println();
        logiciel.print();
        // cela se charge bien


        logiciel.affiche_contenu();

        // cela marche bien


    }




}

