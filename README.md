# Assembly Sub-Compiler 65(C)02

This project is a 65(C)02 compiler at heart, with Java and C++ inspired syntax additions and various helper functions. Accompanied by an IDE for easy editing of sprites, tiles, and general data.

The compiler's basic principles:

* A handlful of flexible regexes identify `function()`, `function(x, y, ...)`, command `{ ... }` blocks, and `labels:`.
* Each scope `{ }` invokes a new sub-compiler with known (or `null` if appropriate) symbol history.
* Any unknown symbols are recorded in real-time and post-filled as soon as they are defined.
* Groovy is used to evaluate symbols which allows for inline arithmetic operations and simple inline function evaluation.

It's not been tested properly but I will be performing that in due course. Please report any bugs if you find any.

Eventually I hope to abstract addressing modes so that, given the necessary Plugin, it can compile for any CPU.

Current features:
---
* Java / C++ inspired assembly language syntax and comments
* Simplified branching, and scope delineation
* Groovy support (write inline Java / Groovy code to generate data on the spot & inject as machine code)
* Inline Huffman compression / decompression
* VERA graphics support (Commander X16)
* Current plugins include `6502`, `65C02`, `StandardLibrary`, and `VeraLibrary`
* Sprite, Tile & Map editing

Getting started:
---

* Create a new `ROOT` folder to store your assembly project files.
* Source assembly `.asm` files should be stored in `ROOT/src/`.
* Use `.export(BIN)` to compile `.asm` files to `.bin` files stored in `ROOT/bin/`; if you don't include this the compiler will compile to a `.prg` and store it in `ROOT/prg/` folder, preprending the two header bytes `$01 $08`. You can change the header by using `.export(PRG, $1234)`.
* You can reference any `.asm` file (no extension required) inside other `.asm` files, e.g. to insert an automatic Huffman decompressor for `tiledata.bin` into your assembly code, or insert automatic Vera decompression upload code.
* Before compiling your `.asm` files make sure to compile any dependency `.asm` files that will be exported as `.bin` files, otherwise the compiler won't have generated the `bin/` binaries you may have referenced in your `.asm` files.

Once compiled a typical project structure might be:

```
	ROOT/
		src/
			spritedemo.asm		// e.g. see example code below

			spritedata.asm		// e.g. ".export(BIN)
							 .image(sprite1, 32)
							 .image(sprite2, 32)
							 .image(sprite3, 32)"
							 
			tiledata.asm		// e.g. ".export(BIN)
							 .image(tile1, 16)
							 .image(tile2, 16)"
		
			map.asm			// e.g. ".export(BIN)
							 .image(map, 256)"
		
		bin/
			spritedata.bin		// generated on compiling spritedata.asm
			tiledata.bin		// generated on compiling tiledata.asm
			map.bin			// generated on compiling map.asm
		
		prg/
			spritedemo.prg		// generated on compiling spritedemo.asm
		
		images/
			sprite1			// raw RGB data 32x32 pixels
			sprite2
			sprite3
			tile1			// raw RGB data 16x16 pixels
			tile2
			map			// raw map data
```

* The `images` folder contains raw data file "images" listed in e.g. `spritedata.asm`, `tiledata.asm` and `map.reasms` which are used to generate `spritedata.bin`, `tiledata.bin` and `map.bin` respectively.
* All raw data including sprites, tiles, maps, and any other data are considered to be "images", and are effectively just an array of 8-bit integers, e.g. sprites and tile image data would be consecutive RGB triples, whereas map data might just be consecutive rows containing integer tile references.
* The project works via the native OS file system so you can choose to create your projects manually and edit your code using e.g. Microsoft Code or whatever, or you can use the provided IDE. Likewise for your graphics - you can build your `graphics.asm` file lists and edit your graphics in the IDE, or you can just add your graphics files to the `ROOT/bin/` folder manually. The choice is yours !

# Example

```
.org($0800)
.prgBasicEntry()

.define(RES_320X240,64)

.org($0810)

.vera_video_mode(RES_320X240, 40, 40, 0)
.vera_sprites_on()

.vera_upload_init(1, 1, $fc00, 0) // bank 1, auto increment 1, address, use data0
.vera_decompress_upload(sprData, 0) // decompress "sprData" and upload directly to vera

.vera_upload_init(0, 1, $4000, 0) // bank 0, auto increment 1, address, use data0
.vera_decompress_upload(spr, 0) // decompress "spr" and upload directly to vera

.vera_upload_init(0, 1, $A000, 0)
.vera_decompress_upload(tiles1, 0) // decompress "tiles1" and upload directly to vera

.vera_upload_init(0, 2, $5000, 0)
.vera_decompress_upload(map1, 0) // decompress "map1" and upload directly to vera

.vera_upload_init(0, 2, $5001, 0) // zero out the alternate bytes
.vera_decompress_upload(zero1, 0) // decompress "zero1" and upload directly to vera

.vera_level_init(1, 64, 8, 40, 16, 20)	// initialise the level

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

# TODO

* Finalise `.asm` IDE editing
* Test & upload existing IDE & compiler

# In the works

* Full list of command support docs
* Additional language syntax and functions
* Groovy sub-compiler support (construct assembly code inside your Groovy code inside your `.asm` files, compile it inside Groovy, and have it inject directly into the `.asm` machine code binary)
* Self-modifying code safe guards
* Inline function support
* Alow users to create their own Plugins
* Abstract addressing modes so it can handle any CPU given the necessary Plugin !

# Support Appreciated

Bitcoin (BTC): `1PifMfpb1T9R3W9rU7VG2Tb13hbVsHkx59`

Ethereum (ETH): `0x8CC167E266228716F8D1343d9d94158b062B14D8`

[MetaHash](https://www.metahash.org) (MHC): `0x008e5761eb66615477149296418d052022596c16b1df3a335d`

Ripple (XRP Network): `rEb8TK3gBgk5auZkwc6sHnwrGVJH8DuaLh` memo: `100907859`
