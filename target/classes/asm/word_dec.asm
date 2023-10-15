// VAR0 - address to decrease
// VAR1 - lower boundary

lda VAR0+1    		// respect the lower boundary
cmp #hi(VAR1)
bne+ {
    lda VAR0
    cmp #lo(VAR1)
    beq finish
}

dec VAR0		// move up
lda VAR0
cmp #255
bne+ {
    dec VAR0+1
}

finish:
