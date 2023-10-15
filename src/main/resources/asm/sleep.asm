// VAR0 - max count

ldx #0

count1:
inx
ldy #0

count2:
iny
cpy #VAR0
bne count2

cpx #VAR1
bne count1


