/**
 * Created by Julien on 11/2/2015.
 */
/*
 * Created on Nov 2, 2015
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */



public class DisquePhysique {

    private final int blocSize = 64;
    private int size;
    private char[][] data;

    public DisquePhysique(int size) {
        this.size=size;
        this.data = new char[size][this.blocSize];
    }

    public void write(int index, char[]d) {
        for(int i=0; i<this.blocSize; i++) {
            this.data[index][i] = d[i];
        }
    }

    char[] read(int index) {
        char[] d = new char[this.blocSize];

        for(int i=0; i<this.blocSize; i++) {
            d[i] = this.data[index][i];
        }

        return d;
    }

    public int getBlocSize() {
        return blocSize;
    }





}
