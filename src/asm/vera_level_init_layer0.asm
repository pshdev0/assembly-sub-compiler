lda $9f29
ora #LAYER_FLAG
sta $9f29
lda #CONFIG
sta $9f2d   // l0_config
lda #MAPBASE
sta $9f2e   // l0_mapbase
lda #TILEBASE
sta $9f2f   // l0_tilebase
