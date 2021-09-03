# Assembly Sub-Compiler 65(C)02

This project is a 65(C)02 compiler at heart, with Java and C++ inspired syntax additions and various helper functions. Accompanied by an IDE for easy data file editing, e.g. sprites, tiles, data, etc.

The compilers basic principles are:

* Each scope `{ }` invokes a new sub-compiler with known (or null if appropriate) symbol history.
* A handlful of flexible regexes identify `functions()`, `function(x, y, ...)`, command `{ ... }` blocks, and `labels:`.
* Any unknown symbols are recorded in real-time and post-filled as soon as they become defined.
* Groovy is used to evaluate symbols which allows for inline arithmetic operations and simple inline function evaluation.

It's not been tested properly but I will be performing that in due course. Please report any bugs if you find any.

Current features:
---
* Java / C++ inspired assembly language syntax and comments
* Simplified branching, and scope delineation
* Groovy support (write inline Java / Groovy code to generate data on the spot & inject as machine code)
* Inline Huffman compression / decompression
* VERA graphics support (Commander X16)
* Sprite, Tile & Map editing
* Plugin based, currently `6502`, `65C02`, `StandardLibrary`, and `VeraLibrary` (with more to come and even user-defined Custom Plugin support :-)

Getting started:
---

* Create a new `ROOT` folder to store your assembly project files.
* Assembly `.asm` and resource `.res` files should be stored in `ROOT/src/` folder. 
* Assembly code you intend to compile into executable `.prg` files should have `.asm` extension, e.g. `ROOT/src/spritedemo.asm` will be compiled and generate `ROOT/prg/spritedemo.prg` with a two-byte header `$10 $08` added to the beginning of the binary. You can load and run this directly in your emulator or physical device.
* Resource binaries you intend to use for data should have `.res` extension, e.g. `ROOT/src/tiledata.res` will be compiled just like `.asm` files, but it will be generated in `ROOT/bin/tiledata.bin` without the `$10 $08` two-byte header.
* You can reference `.res` resource files (no extension required) inside your `.asm` files, e.g. to insert an automatic Huffman decompressor for `tiledata.bin` into your assembly code, or insert automatic Vera decompression upload code. Very handy !
* Before compiling your `.asm` files make sure to compile all your `.res` files, otherwise the compiler won't have generated the `bin/` binaries from the `.res` files which you may have referenced in your `.asm` files.
* Resrouce `.res` files are just `.asm` files with a special purpose so you can mix data and code in there if you like depending on your needs !

Once compiled a typical project structure might be:

```
	ROOT/
		src/
			spritedemo.asm		// e.g. see example code below

			spritedata.res		// e.g. ".image(sprite1, 32)
							 .image(sprite2, 32)
							 .image(sprite3, 32)"
							 
			tiledata.res		// e.g. ".image(tile1, 16)
							 .image(tile2, 16)"
		
			map.res			// e.g. ".image(map, 256)"
		
		bin/
			spritedata.bin		// generated on compiling spritedata.res
			tiledata.bin		// generated on compiling tiledata.res
			map.bin			// generated on compiling map.res
		
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

* The `images` folder contains raw data file "images" referenced by e.g. `spritedata.res`, `tiledata.res` and `map.res` which are used to generate `spritedata.bin`, `tiledata.bin` and `map.bin` respectively.
* All raw data including sprites, tiles, maps, and any other data are considered to be "images", and are effectively just an array of 8-bit integers, e.g. sprites and tile image data would be consecutive RGB triples, whereas map data might just be consecutive rows containing integer tile references.
* Raw image files can be added manually to `images/` or you can use the IDE to open your project `ROOT/` folder and manage data files that way. You define your data in the `.res` files, e.g. you can create your data in the IDE or on the command line and if you open up your ROOT folder in the IDE it will recognise your `images/` files so long as you've specified them in resource `.res` files in `src/`.
* You can compile manually on the command line or use the IDE; the compiler and IDE were written to be as flexible as possible and in fact the IDE invokes the command line compiler anyway.

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
