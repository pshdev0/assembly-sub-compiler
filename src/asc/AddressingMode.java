package asc;

public class AddressingMode {
   String name;
   public String regex;
   public int start;
   public int end;
   public AddressingModeLambda func;

   public AddressingMode(String name, String regex, AddressingModeLambda func) {
      this.name = name;
      this.regex = regex;
      this.func = func;
   }
}
