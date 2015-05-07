
import java.util.logging.Level;
import java.util.logging.Logger;
import jnpout32.pPort;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Administrador
 */
public class IoTest {

    static short datum;
    static short Addr;
    static pPort lpt;

    public void do_write() {
        // Notify the console
        System.out.println("Write to Port: " + Integer.toHexString(Addr)
                + " with data = " + Integer.toHexString(datum));
        //Write to the port
        lpt.output(Addr, datum);
    }

    public int do_read() {
        // Read from the port
        datum = (short) lpt.input(Addr);

        // notifiacion a la consola 
        System.out.println("Read Port: " + Integer.toHexString(Addr)
                + " = " + datum);

        
        return datum;
    }

   

}
