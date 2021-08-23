# 65C02-asm-compiler
My own 65(C)02 language compiler with

* Modern Java/C++ syntax and scope delineation
* Groovy support with sub-compiler support
* Self-modifying code safe guards
* Vera graphics support (Commander X16)
* Automatic Huffman compression / decompression

TODO - upload !

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


