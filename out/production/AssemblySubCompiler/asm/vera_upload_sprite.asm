lda VAR0      // load lo byte pointer
sta $9f23 + VAR1
lda VAR0+1    // load hi byte pointer
and #15       // clear top 4 bits
adc VAR0+2    // add on the bpp bit
sta $9f23 + VAR1
lda VAR0+3    // load x lo
sta $9f23 + VAR1
lda VAR0+4    // load x hi
sta $9f23 + VAR1
lda VAR0+5    // load y lo
sta $9f23 + VAR1
lda VAR0+6    // load y hi
sta $9f23 + VAR1
lda VAR0+7    // load depth
sta $9f23 + VAR1
lda VAR0+8    // load size
sta $9f23 + VAR1
