ldx #0
loop:
  lda VAR0, x
  sta $9f23 + VAR1
  inx
  cpx #VAR2
  bne loop
