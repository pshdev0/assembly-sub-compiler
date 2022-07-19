// VAR0 - scale
// VAR1 - color
// VAR2 - hborder
// VAR3 - vborder

lda $9F25              // set DCSEL=0 (duplicated registers flag)
and #255-2
sta $9F25

lda #VAR0              // double pixels
sta $9f2a              // set H_SCALE
sta $9f2b              // set V_SCALE
lda #VAR1              // set the border color
sta $9f2c

lda $9F25              // we need to set DCSEL=1 to access duplicated registers
ora #2
sta $9F25

lda #VAR2/4            // set screen x1 to 40/640*320 (drop low 2 bytes -> / 4)
sta $9f29
lda #(640-VAR2)/4      // set screen x2 to 600/640*320 (drop low 2 bytes -> /4)
sta $9f2a
lda #VAR3/2            // set screen y1 to 40/480*240 (drop low 1 byte -> /2)
sta $9f2b
lda #(480-VAR3)/2      // set screen y2 to 440/480*240 (drop low 1 byte -> /2)
sta $9f2c

lda $9F25              // restore DCSEL=0 (duplicated registers flag)
and #255-2
sta $9F25
