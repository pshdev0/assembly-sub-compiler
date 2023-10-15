lda #VAR0
sta $9f22
lda #lo(VAR1) // vera lo pointer
sta $9f20
lda #hi(VAR1) // vera hi pointer
sta $9f21
lda #VAR2     // use data0 or data1
sta $9f25
