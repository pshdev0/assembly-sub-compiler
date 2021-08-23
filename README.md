# 65(C)02-asm-compiler
My own 65(C)02 assembly language compiler with Java/JavaFX IDE

Currently supports:

* Modern Java/C++ syntax and scope delineation
* Groovy support
* Vera graphics support (Commander X16)
* Sprite, Tile & Map editing
* Inline Huffman compression / decompression

Todo:

* Test & upload existing IDE & assembler

In the works:

* Self-modifying code safe guards
* Groovy sub-compiler support
* Additional language syntax and functions
* Full list of command support docs to follow

This project is a 65(C)02 compiler at heart, with syntax additions inspired by Java and C++, and with advanced library functions. I wanted to create a language which could be used for pure 65(C)02 programming, but also providing a modern language style and advanced functions should you want to use them.

# Simple example

```
.export(X16TEST)

.org($0800)
.prgBasicEntry()

.define(RES_320X240,64)

.org($0810)

.vera_video_mode(RES_320X240, 40, 40, 0)
.vera_sprites_on()

.vera_upload_init(1, 1, $fc00, 0) // bank 1, auto increment 1, address, use data0
.vera_decompress_upload(sprData, 0)

.vera_upload_init(0, 1, $4000, 0) // bank 0, auto increment 1, address, use data0
.vera_decompress_upload(spr, 0)

.vera_upload_init(0, 1, $A000, 0) // upload tiles
.vera_decompress_upload(tiles1, 0)

.vera_upload_init(0, 2, $5000, 0) // upload map
.vera_decompress_upload(map1, 0)

.vera_upload_init(0, 2, $5001, 0) // zero out the alternate bytes
.vera_decompress_upload(zero1, 0)

// todo 128x128 & 256x256 need debugging
// switch inc/dec to 2 bytes length and apply min/max
.vera_level_init(1, 64, 8, 40, 16, 20)	//

.forever {

	jsr $ff53 			// joystick_scan
	lda #0
	jsr $ff56 			// joystick_get
	
	tax
	and #8	  // up
	bne+ {
		.halfword--($9f39, 0)	// move up with lower bound
	}

	txa
	and #4	  // down
	bne+ {
		.halfword++($9f39, 816)	// move down with upper bound
	}

	txa
	and #2	  // left
	bne+ {
		.halfword--($9f37, 0)	// move left with lower bound
	}
	
	txa
	and #1	  // right
	bne+ {
		.halfword++($9f37, 736)	// move right with upper bound
	}
	
	.sleep(20, 20)
}
```

# Support

If you like this project, please consider supporting it here:

Bitcoin address (BTC): 1PifMfpb1T9R3W9rU7VG2Tb13hbVsHkx59
Ethereum address (ETH): 0x8CC167E266228716F8D1343d9d94158b062B14D8
MetaHash address (MHC): 0x008e5761eb66615477149296418d052022596c16b1df3a335d
Ripple address (XRP Network): rEb8TK3gBgk5auZkwc6sHnwrGVJH8DuaLh (memo: 100907859)
