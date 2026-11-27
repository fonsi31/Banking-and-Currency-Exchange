/********************
Last names: Marcelino, Baun, Cauilan, Dimakuta,
Language: Java
Paradigm(s): Object-Oriented (with a small amount of structured/imperative code inside methods)

Compile: javac *.java
Run:     java MP_2_Java
********************/

import java.util.NoSuchElementException;

public class MP_2_Java
{
    public static void main(String[] args)
    {
        try
        {
            new BankApp().run();
        }
        catch (NoSuchElementException e) // console input was closed (Ctrl+D / end of piped input)
        {
            System.out.println();
            System.out.println("Input closed. Goodbye.");
        }
    }
}
