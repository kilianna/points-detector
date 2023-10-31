package io.github.kildot.backgroundSubtractor;

import java.awt.Color;
import javax.swing.InputVerifier;


public class Verifiers {
    
    public static abstract class ActiveInputVerifier extends InputVerifier {
        @Override
        public boolean shouldYieldFocus(javax.swing.JComponent input) {
            boolean ok = verify(input);
            input.setBackground(ok ? Color.WHITE : new Color(0xFFAA99));
            return ok;
        }
    }

    public static abstract class IntegerInputVerifier extends ActiveInputVerifier {
        private int min;
        private int max;

        public IntegerInputVerifier(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public IntegerInputVerifier() {
            min = -0x7FFFFFFF;
            max = 0x7FFFFFFF;
        }

        public boolean verify(int value) {
            return min <= value && value <= max;
        }

        @Override
        public boolean verify(javax.swing.JComponent input) {
            try {
                int value = Integer.parseInt(((javax.swing.JTextField)input).getText());
                return verify(value);
            } catch (Exception ex) {
                return false;
            }
        }        
    }
}
