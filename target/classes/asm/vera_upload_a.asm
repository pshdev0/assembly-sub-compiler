// VAR0 - address
// VAR1 - data01 - todo (currently unused)
// VAR2 - size

loop:
{
   lda selfmodify1:VAR0   // get the data ($1234 is self modify; avoid $0000 zp bug !)
   sta $9f23              // upload to vera

   // increment the pointer
   lda selfmodify1   // load lo byte
   clc               // increase it and check for carry
   adc #1
   bcc+
   {
      inc selfmodify1+1   // carry, so increment
   }
   sta selfmodify1   // write our increment back where it should be
   cmp goal      // check hi byte
   bne loop
   {
      lda selfmodify1+1
      cmp goal+1   // check lo byte
      {
         bne loop
      }

      jmp exit
   }
}

goal:
   .byte { lo(VAR0 + VAR2), hi(VAR0 + VAR2) }

exit:
