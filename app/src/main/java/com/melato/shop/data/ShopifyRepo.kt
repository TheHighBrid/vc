package com.melato.shop.data

import com.melato.shop.data.model.Category
import com.melato.shop.data.model.FaqItem
import com.melato.shop.data.model.PopupEvent
import com.melato.shop.data.model.Product

private const val CDN = "https://cdn.shopify.com/s/files/1/0809/3358/5151/files/"

object ShopifyRepo {

    val categories = listOf(
        Category("all", "All"),
        Category("tracksuits", "Tracksuits"),
        Category("denim", "Denim"),
        Category("tops", "Tops"),
        Category("accessories", "Accessories"),
        Category("eves-wardrobe", "Eve's Wardrobe")
    )

    val popupEvents = listOf(
        PopupEvent("June 21", "Location announced 48h prior"),
        PopupEvent("July 21", "Location announced 48h prior"),
        PopupEvent("August 20", "Location announced 48h prior")
    )

    val products = listOf(
        // ── TRACKSUITS ──────────────────────────────────────────────────────────
        Product(
            id = "ojos-jacket",
            handle = "ojos-velour-track-jacket",
            title = "OJOS Velour Track Jacket",
            price = 109.99,
            description = "A premium velour track jacket featuring a burgundy, black, red, and mauve colorblock design. The piece includes a back graphic reading \"The Eyes Chico They Never Lie\" and branded sleeve embroidery. Designed as part of a matching tracksuit set.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Ojos"),
            isFeatured = true,
            tags = listOf("Bestseller"),
            imageUrl = "${CDN}77963_672f7575-e89e-4633-84c4-1a9113e32c24.jpg",
            accent = 0xFFB44040
        ),
        Product(
            id = "ojos-pant",
            handle = "ojos-velour-track-pants",
            title = "OJOS Velour Track Pants",
            price = 69.99,
            description = "Matching track pant to the OJOS Velour Track Jacket. Colorblock velour with elastic waist, tapered leg, and branded details.",
            category = "tracksuits",
            sizes = listOf("One Size"),
            colors = listOf("Rose Gold"),
            tags = listOf("Pair with jacket"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_b20f8220-a544-4191-9306-ecda6711ceea.png",
            accent = 0xFFB44040
        ),
        Product(
            id = "ovum-jacket",
            handle = "ovum-satin-track-jacket-pearl",
            title = "OVUM Satin Track Jacket — Pearl",
            price = 159.99,
            description = "A premium track jacket featuring clean retro-sport structure and controlled color blocking. Built as the upper half of the Melato uniform: structured enough to hold shape, relaxed enough to move through the day.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Pearl"),
            isFeatured = true,
            isNew = true,
            tags = listOf("New Drop", "OVUM"),
            imageUrl = "${CDN}melato-ovum-satin-track-jacket-pearl.jpg",
            accent = 0xFFD4C9A8
        ),
        Product(
            id = "ovum-pant",
            handle = "ovum-satin-track-pant-pearl",
            title = "OVUM Satin Track Pant — Pearl",
            price = 149.99,
            description = "The matching satin track pant to the OVUM Jacket. Pearl finish, elastic waistband with adjustable drawstring, tapered leg.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Pearl"),
            isNew = true,
            tags = listOf("New Drop", "OVUM"),
            imageUrl = "${CDN}melato-ovum-satin-track-pant-pearl.jpg",
            accent = 0xFFD4C9A8
        ),
        Product(
            id = "divididos-jacket",
            handle = "divididos-velour-track-jacket",
            title = "Divididos Velour Track Jacket",
            price = 89.99,
            description = "Heritage-inspired velour construction with a distinctive divided colorblock layout. Full front zip, structured collar, branded embroidery. Pairs with the Divididos Track Pant for the full set.",
            category = "tracksuits",
            sizes = listOf("S", "M", "L", "XL"),
            colors = listOf("Divididos"),
            isFeatured = true,
            tags = listOf("Core"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_c6111df6-11a0-4d6f-af91-e1f5119e7cc7.jpg",
            accent = 0xFF5E4B8B
        ),
        Product(
            id = "divididos-pant",
            handle = "divididos-velour-track-pant",
            title = "Divididos Velour Track Pant",
            price = 59.99,
            description = "Matching track pant to the Divididos Velour Track Jacket. Heritage colorblock design, elastic waist, side pockets.",
            category = "tracksuits",
            sizes = listOf("S", "M", "L", "XL"),
            colors = listOf("Divididos"),
            imageUrl = "${CDN}melato-divididos-velour-track-pant-front.png",
            accent = 0xFF5E4B8B
        ),
        Product(
            id = "conquista-jacket",
            handle = "conquista-velour-track-jacket",
            title = "Conquista Velour Track Jacket",
            price = 89.99,
            description = "Mission-coded color-blocking with controlled presence. Teal, purple, lime, orange, burgundy, and black colorblock layout with graphic back detailing. Relaxed athletic fit — choose true size or size up.",
            category = "tracksuits",
            sizes = listOf("S", "M", "L", "XL"),
            colors = listOf("Conquista"),
            isFeatured = true,
            tags = listOf("Bold"),
            imageUrl = "${CDN}melato-conquista-velour-track-jacket-alternate.png",
            accent = 0xFF2A9D8F
        ),
        Product(
            id = "conquista-pant",
            handle = "conquista-velour-track-pant",
            title = "Conquista Velour Track Pant",
            price = 59.99,
            description = "Matching pant to the Conquista Track Jacket. Bold colorblock construction, elastic waist with drawstring, tapered leg.",
            category = "tracksuits",
            sizes = listOf("S", "M", "L", "XL"),
            colors = listOf("Conquista"),
            imageUrl = "${CDN}melato-conquista-velour-track-pant-front.png",
            accent = 0xFF2A9D8F
        ),
        Product(
            id = "chu-jacket",
            handle = "chu-velour-track-jacket",
            title = "Chū Velour Track Jacket",
            price = 89.99,
            description = "Grey blue geometric colorblock velour jacket with structured silhouette. A quieter expression of the Melato tracksuit identity — still intentional, still limited.",
            category = "tracksuits",
            sizes = listOf("S", "M", "L", "XL"),
            colors = listOf("Chū"),
            imageUrl = "${CDN}melato-chu-velour-track-jacket-alternate.png",
            accent = 0xFF6B8CAE
        ),
        Product(
            id = "chu-pant",
            handle = "chu-velour-track-pant",
            title = "Chū Velour Track Pant",
            price = 59.99,
            description = "Matching track pant to the Chū Velour Jacket. Grey blue geometric colorblock, elastic waist, tapered cut.",
            category = "tracksuits",
            sizes = listOf("S", "M", "L", "XL"),
            colors = listOf("Chū"),
            imageUrl = "${CDN}melato-chu-velour-track-pant-front.png",
            accent = 0xFF6B8CAE
        ),
        Product(
            id = "passion-jacket",
            handle = "passion-fruit-velour-track-jacket",
            title = "Passion Fruit Velour Track Jacket",
            price = 89.99,
            description = "Earth tone, warm colorblock velour jacket. One of the more expressive pieces in the tracksuit lineup — worn alone or as a full set.",
            category = "tracksuits",
            sizes = listOf("S", "M", "L", "XL"),
            colors = listOf("Passion Fruit"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_f475785a-aff5-4826-8c6c-2a16cf37d44b.png",
            accent = 0xFFE07B39
        ),
        Product(
            id = "passion-pant",
            handle = "passion-fruit-velour-track-pant",
            title = "Passion Fruit Velour Track Pant",
            price = 59.99,
            description = "Matching pant to the Passion Fruit Velour Track Jacket. Earth tone colorblock, relaxed athletic fit.",
            category = "tracksuits",
            sizes = listOf("S", "M", "L", "XL"),
            colors = listOf("Passion Fruit"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_ce964744-d66c-4a02-977d-a405915e7bc8.jpg",
            accent = 0xFFE07B39
        ),
        Product(
            id = "midas-jacket",
            handle = "midas-runner-track-jacket",
            title = "Midas Runner Track Jacket",
            price = 149.99,
            description = "The runner silhouette — lighter construction, longer hang, athletic performance shape. The Midas jacket sits above the core velour line in feel and finish.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Midas"),
            isFeatured = true,
            tags = listOf("Premium"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_7986447b-7784-4dec-9b07-842914a3a3df.png",
            accent = 0xFFC9A84C
        ),
        Product(
            id = "midas-pant",
            handle = "midas-runner-track-pants",
            title = "Midas Runner Track Pants",
            price = 89.99,
            description = "Matching runner track pant to the Midas jacket. Lightweight athletic construction, tapered leg, elastic waist with cord.",
            category = "tracksuits",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Midas"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_7834f71d-51bd-4a28-b1d8-cd33929d1fce.png",
            accent = 0xFFC9A84C
        ),
        Product(
            id = "reliance-jacket",
            handle = "reliance-bamboo-track-jacket",
            title = "Reliance Bamboo Track Jacket",
            price = 99.99,
            description = "Bamboo-blend track jacket in black salmon colorblock. Softer hand than velour — worn close or relaxed. The Reliance set sits between performance and leisure.",
            category = "tracksuits",
            sizes = listOf("S", "M", "L", "XL"),
            colors = listOf("Black Salmon"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_1964916b-e416-48f1-a69d-9cb84f34f43e.png",
            accent = 0xFFE8A598
        ),
        Product(
            id = "reliance-pant",
            handle = "reliance-bamboo-track-pant",
            title = "Reliance Bamboo Track Pant",
            price = 59.99,
            description = "Bamboo-blend matching track pant. Black salmon colorblock, elastic waist, tapered leg.",
            category = "tracksuits",
            sizes = listOf("S", "M", "L", "XL"),
            colors = listOf("Black Salmon"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_0c141d33-bb82-423a-8fbb-c78fd499336d.jpg",
            accent = 0xFFE8A598
        ),

        // ── DENIM ───────────────────────────────────────────────────────────────
        Product(
            id = "9-to-noir",
            handle = "9-to-noir-tailored-flare-pant",
            title = "9 to Noir Tailored Flare Pant",
            price = 74.99,
            description = "A tailored flare silhouette in deep noir. Cut with precision — from the office to the after. One of the most versatile pieces in the collection.",
            category = "denim",
            sizes = listOf("S", "M", "L"),
            colors = listOf("Noir"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_2f725dc5-48e3-4781-bcaa-1763225b353e.jpg",
            accent = 0xFF1A1A2E
        ),
        Product(
            id = "hendrix-jean",
            handle = "hendrix-distressed-flare-jean",
            title = "Hendrix Distressed Flare Jean",
            price = 79.99,
            description = "Distressed flare denim with a vintage athletic soul. The Hendrix sits at the intersection of seventies denim culture and modern street — worn with anything.",
            category = "denim",
            sizes = listOf("28", "30", "32", "34", "36"),
            colors = listOf("Hendrix"),
            isFeatured = true,
            imageUrl = "${CDN}rn-image_picker_lib_temp_593dec29-b17f-4f96-abfd-acbf64de4190.png",
            accent = 0xFF3D5A80
        ),
        Product(
            id = "berry-jean",
            handle = "berry-distressed-slim-jean",
            title = "Berry Distressed Slim Jean",
            price = 139.99,
            description = "Slim cut distressed denim in a rich berry wash. A premium take on the classic slim — the distressing is intentional, not decorative.",
            category = "denim",
            sizes = listOf("28", "30", "32", "34", "36"),
            colors = listOf("Berry"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_c33e3385-56f5-4150-b1dd-57c90db51f1b.png",
            accent = 0xFF6B2D5E
        ),
        Product(
            id = "dirt-rich",
            handle = "dirt-rich-denim-flare-jean",
            title = "Dirt Rich Denim Flare Jean",
            price = 89.99,
            description = "Raw-edge flare denim with earth-toned washes and heavy character. The Dirt Rich is built for those who wear their clothes, not just own them.",
            category = "denim",
            sizes = listOf("24", "26", "28", "30", "32"),
            colors = listOf("Dirt Rich"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_f57c49d5-d437-4762-9e2a-f6a0dfb03533.jpg",
            accent = 0xFF7B5E3C
        ),
        Product(
            id = "nuit-blanche",
            handle = "nuit-blanche-zip-flare-jean",
            title = "Nuit Blanche Zip Flare Jean",
            price = 85.99,
            description = "Zip-detail flare denim for the late-night crowd. The Nuit Blanche is a statement piece — wear it when the night calls for something more.",
            category = "denim",
            sizes = listOf("28", "30", "32", "34", "38"),
            colors = listOf("Nuit Blanche"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_1a0b9127-132e-4722-a6ee-fbe2a349d6c4.png",
            accent = 0xFF2C3E50
        ),
        Product(
            id = "off-white-lie",
            handle = "off-white-lie-flare-jean",
            title = "Off-White Lie Flare Jean",
            price = 89.99,
            description = "A pale, off-white flare denim that reads clean and deliberate. The Off-White Lie is effortless dressed up or down.",
            category = "denim",
            sizes = listOf("24", "26", "28", "30", "32"),
            colors = listOf("Off-White"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_7aba15e3-b953-40b3-ba8d-73a7a596e973.jpg",
            accent = 0xFFE8E4D9
        ),
        Product(
            id = "rosetta-jean",
            handle = "rosetta-distressed-flare-jean",
            title = "Rosetta Distressed Flare Jean",
            price = 79.99,
            description = "A feminine-leaning distressed flare with warm undertones. The Rosetta pairs easily across the collection — a dependable piece in any rotation.",
            category = "denim",
            sizes = listOf("28", "30", "32", "34", "36"),
            colors = listOf("Rosetta"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_e074f113-082a-4e38-afa3-e1c5cfc3c7d8.png",
            accent = 0xFFB07560
        ),
        Product(
            id = "noir-traffic",
            handle = "noir-traffic-wide-jean",
            title = "Noir Traffic Wide Jean",
            price = 79.99,
            description = "A wide-leg silhouette in noir with graphic detail. Relaxed fit, unisex sizing. The Noir Traffic is the daily denim for those who move differently.",
            category = "denim",
            sizes = listOf("S", "M", "L", "XL", "2XL"),
            colors = listOf("Black"),
            isNew = true,
            tags = listOf("New"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_9e74971b-f52b-440a-adc9-c1e8301fba21.png",
            accent = 0xFF1A1A1A
        ),
        Product(
            id = "casanegra",
            handle = "casanegra-leather-flare-zip-pants",
            title = "CasaNegra Leather Flare Zip Pants",
            price = 159.99,
            description = "Premium leather-look flare zip pants in deep black. The CasaNegra is a full presence piece — structured silhouette, bold finish, built to own a room.",
            category = "denim",
            sizes = listOf("28", "29", "30", "32", "34"),
            colors = listOf("Nero"),
            isFeatured = true,
            tags = listOf("Premium"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_9db4135b-f026-4caa-a943-a03e7836d978.png",
            accent = 0xFF1C1C1C
        ),
        Product(
            id = "kech",
            handle = "kech-leather-flare-zip-pants",
            title = "Kech Leather Flare Zip Pants",
            price = 159.99,
            description = "Leather-look flare zip pants in a warmer tonal palette — Casablanca colorway. The Kech offers the same premium construction as the CasaNegra with a different visual language.",
            category = "denim",
            sizes = listOf("28", "29", "30", "32", "34"),
            colors = listOf("Casablanca"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_b005d4b3-c76c-4979-b81c-6dfd0bfddaec.png",
            accent = 0xFF8B6914
        ),

        // ── TOPS ────────────────────────────────────────────────────────────────
        Product(
            id = "barolo",
            handle = "barolo-tweed-zip-overshirt",
            title = "Barolo Tweed Zip Overshirt",
            price = 149.99,
            description = "A rich tweed zip overshirt in deep burgundy tones — the Barolo is designed to layer, to lead, and to last. Textured construction with a clean front zip.",
            category = "tops",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Barolo"),
            isFeatured = true,
            imageUrl = "${CDN}rn-image_picker_lib_temp_c959cd8c-ff24-4b2b-a1fa-7b5ae868ac39.png",
            accent = 0xFF722F37
        ),
        Product(
            id = "bluefin",
            handle = "the-bluefin-relaxed-shirt",
            title = "Bluefin Relaxed Shirt",
            price = 45.99,
            description = "A clean, relaxed button-up in a quiet palette. The Bluefin is the everyday reach — effortless and considered without demanding attention.",
            category = "tops",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Bluefin Stripe"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_598395a9-163e-4f2d-b31d-e5ed6ad33f23.png",
            accent = 0xFF4A7FA5
        ),
        Product(
            id = "moula",
            handle = "moula-caramel-cable-knit-polo",
            title = "Moula Caramel Cable Knit Polo",
            price = 129.99,
            description = "A cable knit polo in warm caramel tones. The Moula is luxury dressed casually — premium tactile construction, refined silhouette.",
            category = "tops",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Caramel"),
            isFeatured = true,
            isNew = true,
            tags = listOf("New"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_5c622955-3d2e-47b6-9d63-cd0c09c8b715.jpg",
            accent = 0xFFC8903A
        ),
        Product(
            id = "olive-affair",
            handle = "olive-affair-retro-panel-jacket",
            title = "Olive Affair Retro Panel Jacket",
            price = 84.99,
            description = "A retro-paneled jacket in olive tones with a structured athletic cut. The Olive Affair is a utility piece built for daily movement.",
            category = "tops",
            sizes = listOf("S", "M", "L"),
            colors = listOf("Olive"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_8efde421-c08e-4c27-840e-dc3073369bd9.png",
            accent = 0xFF556B2F
        ),
        Product(
            id = "taupe-secret",
            handle = "taupe-secret-retro-panel-jacket",
            title = "Taupe Secret Retro Panel Jacket",
            price = 84.99,
            description = "Retro panel jacket in taupe — a quieter take on the same athletic silhouette as the Olive Affair. Pairs across the denim and tracksuit collections.",
            category = "tops",
            sizes = listOf("S", "M", "L"),
            colors = listOf("Taupe"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_712817b7-81a9-4dfe-9b9c-7623e4914fad.jpg",
            accent = 0xFFB5A899
        ),
        Product(
            id = "smoke-signal",
            handle = "smoke-signal-spray-tee",
            title = "Smoke Signal Spray Tee",
            price = 74.99,
            description = "A spray-effect graphic tee with a considered fade. The Smoke Signal is the foundational layer — wear it under a jacket, over a bodysuit, or on its own.",
            category = "tops",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Smoke"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_5a630211-5cae-4784-8d29-e11b5e492bb8.png",
            accent = 0xFF696969
        ),
        Product(
            id = "taqburni",
            handle = "taqburni-velour-polo-shirt",
            title = "Taqburni Velour Polo Shirt",
            price = 99.99,
            description = "A velour polo shirt that bridges the gap between the tracksuit line and tailored tops. The Taqburni is a Melato original — textured, intentional, limited.",
            category = "tops",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Taqburni"),
            isNew = true,
            tags = listOf("New"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_91a9cfbb-ceb9-4542-a08c-9ad837649642.jpg",
            accent = 0xFF8B0000
        ),
        Product(
            id = "mint-tee",
            handle = "mint-condition-panel-tee",
            title = "Mint Condition Panel Tee",
            price = 35.99,
            description = "An oversized panel tee in mint green and white snow colorways. Relaxed unisex fit — the easy statement tee from Melato Studios.",
            category = "tops",
            sizes = listOf("M", "L", "XL"),
            colors = listOf("Mint Green", "White Snow"),
            isNew = true,
            tags = listOf("New"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_8ea818d0-fc15-4f00-9b8d-5eaad4901bd8.png",
            accent = 0xFF4CAF7A
        ),

        // ── ACCESSORIES ─────────────────────────────────────────────────────────
        Product(
            id = "atlas-dopp",
            handle = "atlas-dopp-kit",
            title = "Atlas Dopp Kit",
            price = 69.99,
            description = "A compact, structured travel kit in premium coated canvas with monogram detail. The Atlas Dopp Kit is finished to the same standard as the apparel — carry it with intent.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Atlas"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_b396a57a-a5d3-43b6-be1b-293f78d9d4d3.png",
            accent = 0xFF5C4033
        ),
        Product(
            id = "atlas-duffle",
            handle = "atlas-woven-duffle",
            title = "Atlas Woven Duffle",
            price = 189.99,
            description = "A woven duffle bag with premium hardware and structured construction. Built for the traveler who doesn't compromise — generous capacity, deliberate design.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Atlas"),
            isFeatured = true,
            imageUrl = "${CDN}rn-image_picker_lib_temp_f65ed62e-dada-4c49-9b8b-223c9fee8064.png",
            accent = 0xFF5C4033
        ),
        Product(
            id = "emerald-tie",
            handle = "emerald-solstice-tile-geometric-tie",
            title = "Emerald Solstice Tile Geometric Tie",
            price = 79.99,
            description = "A geometric tile pattern tie in emerald tones. The Emerald Solstice is part of the Zellige capsule — sharp, considered, limited.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Emerald"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_573e449e-fd4b-4299-b40b-927b8d05542f.jpg",
            accent = 0xFF2D6A4F
        ),
        Product(
            id = "knotorious",
            handle = "knotorious-monogram-tie",
            title = "Knotorious Monogram Tie",
            price = 89.99,
            description = "A monogram tie with an all-over Melato pattern. The Knotorious is a statement piece — wearable brand identity at its most deliberate.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Knotorious"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_5ef14989-eb7d-4cad-ae28-7f3160333e5b.jpg",
            accent = 0xFF1B1B2F
        ),
        Product(
            id = "rosewood-tie",
            handle = "rosewood-checkerboard-tie",
            title = "Rosewood Checkerboard Tie",
            price = 79.99,
            description = "Checkerboard pattern tie in rosewood tones. Clean geometry, warm colour — a secondary statement that finishes any formal or semi-formal look.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Rosewood"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_26388381-558a-46d5-a184-8d9d6f37b66a.jpg",
            accent = 0xFF7D3C3C
        ),
        Product(
            id = "ruby-tie",
            handle = "ruby-labyrinth-geometric-tie",
            title = "Ruby Labyrinth Geometric Tie",
            price = 79.99,
            description = "A deep ruby geometric labyrinth tie. Bold pattern, refined execution — the Ruby Labyrinth finishes a look or starts a conversation.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Ruby"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_092c6019-0ff4-46fe-b879-45da3a4a6612.jpg",
            accent = 0xFF8B0000
        ),
        Product(
            id = "sunlit-tie",
            handle = "sunlit-atrium-geometric-tie",
            title = "Sunlit Atrium Geometric Tie",
            price = 79.99,
            description = "A warm, golden-hour geometric tie in sunlit tones. The Atrium is lighter in palette — a versatile accent that works across the collection.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Sunlit"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_86a1b481-6dcc-4547-abf8-46c00ec6e660.png",
            accent = 0xFFC9A84C
        ),
        Product(
            id = "ivy-harness",
            handle = "ivy-trellis-harness-suspenders",
            title = "Ivy Trellis Harness Suspenders",
            price = 79.99,
            description = "Structured harness suspenders in an ivy trellis pattern. Wearable as a fashion accessory or functional piece — the Ivy Trellis is a collector's item.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Ivy"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_d7b1a9ee-f657-40d0-ae74-8c04f65d1005.jpg",
            accent = 0xFF3A6351
        ),
        Product(
            id = "salopette",
            handle = "salopette-de-salo-harness-suspenders",
            title = "Salopette de Salo Harness Suspenders",
            price = 79.99,
            description = "A fashion-forward harness suspender set with architectural construction. The Salopette de Salo is a Melato original — worn as a centrepiece or layering accent.",
            category = "accessories",
            sizes = listOf("One Size"),
            colors = listOf("Salo"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_734dacab-a721-43d9-af41-79ace1525cba.jpg",
            accent = 0xFF4A4A4A
        ),

        // ── EVE'S WARDROBE ───────────────────────────────────────────────────────
        Product(
            id = "blue-hour",
            handle = "blue-hour-alibi-mesh-bodysuit",
            title = "Blue Hour Alibi Mesh Bodysuit",
            price = 199.99,
            description = "A sheer mesh bodysuit designed for the blue hour — the quiet drama between day and night. Premium construction, intentional coverage, an unapologetic silhouette.",
            category = "eves-wardrobe",
            sizes = listOf("S", "M", "L"),
            colors = listOf("Blue Hour"),
            isFeatured = true,
            isNew = true,
            tags = listOf("Eve's Wardrobe"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_de1c346b-e085-4fe2-8789-239027b20b34.jpg",
            accent = 0xFF1A3A5C
        ),
        Product(
            id = "body-language",
            handle = "body-language-mesh-bodysuit",
            title = "Body Language Mesh Bodysuit",
            price = 199.99,
            description = "Mesh bodysuit with deliberate cut and controlled exposure. The Body Language speaks for itself — wear it loud or quiet, but wear it on purpose.",
            category = "eves-wardrobe",
            sizes = listOf("S", "M", "L"),
            colors = listOf("Cocoa"),
            tags = listOf("Eve's Wardrobe"),
            imageUrl = "${CDN}melato-body-language-cocoa-mesh-bodysuit-front.jpg",
            accent = 0xFF5C3D2E
        ),
        Product(
            id = "cognac-skirt",
            handle = "cognac-confession-leather-pencil-skirt",
            title = "Cognac Confession Leather Pencil Skirt",
            price = 130.00,
            description = "A leather pencil skirt in cognac — precise, bold, and entirely deliberate. Pairs with bodysuits, the Moula polo, or anything with presence.",
            category = "eves-wardrobe",
            sizes = listOf("S", "M", "L"),
            colors = listOf("Cognac"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_678fde1f-5e9c-4882-998c-022bfe155574.jpg",
            accent = 0xFF9B5A2A
        ),
        Product(
            id = "noir-alibi",
            handle = "noir-alibi-lace-corset-dress",
            title = "Noir Alibi Lace Corset Dress",
            price = 319.99,
            description = "The statement piece of Eve's Wardrobe. A lace corset dress in noir with structured boning and deliberate silhouette. Limited production — not a restock piece.",
            category = "eves-wardrobe",
            sizes = listOf("S", "M", "L"),
            colors = listOf("Noir"),
            isFeatured = true,
            isNew = true,
            tags = listOf("Eve's Wardrobe", "Statement"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_90c7e6e3-5499-40e6-8f1f-4ec547f31dd4.jpg",
            accent = 0xFF0D0D0D
        ),
        Product(
            id = "noir-memoir",
            handle = "noir-memoir-wrap-pencil-skirt",
            title = "Noir Memoir Wrap Pencil Skirt",
            price = 110.00,
            description = "A wrap pencil skirt in noir with a clean, tailored finish. The Noir Memoir is the understated option in Eve's Wardrobe — every bit as intentional.",
            category = "eves-wardrobe",
            sizes = listOf("S", "M", "L"),
            colors = listOf("Noir"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_b1e85891-55d0-49bf-9656-13ccb456b4b4.jpg",
            accent = 0xFF1C1C1C
        ),
        Product(
            id = "dawn-to-dusk",
            handle = "dawn-to-dusk-three-piece-undergarment-set",
            title = "Dawn-to-Dusk Three Piece Undergarment Set",
            price = 119.99,
            description = "A three-piece undergarment set designed for all-day comfort and intentional styling. The Dawn-to-Dusk is a complete layering solution — morning through night.",
            category = "eves-wardrobe",
            sizes = listOf("S", "M", "L"),
            colors = listOf("Ivory", "Noir"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_93d6752e-fc15-4803-af49-22d5a50e92a4.png",
            accent = 0xFFF5ECD7
        ),
        Product(
            id = "side-note",
            handle = "side-note-lace-rib-top",
            title = "Side Note Lace Rib Top",
            price = 59.99,
            description = "A lace rib top that layers under everything or stands alone. The Side Note is the entry point to Eve's Wardrobe — subtle, tactile, considered.",
            category = "eves-wardrobe",
            sizes = listOf("S", "M", "L"),
            colors = listOf("Ivory", "Noir"),
            imageUrl = "${CDN}rn-image_picker_lib_temp_3899bbd8-e29d-483f-b836-bda0c1a3b8d8.jpg",
            accent = 0xFFD4C5B2
        )
    )

    val faq = listOf(
        FaqItem("How long does order processing take?",
            "Orders are usually prepared within 1 to 3 business days. During limited drops, holidays, or high-volume periods, processing may take slightly longer."),
        FaqItem("How long does shipping take?",
            "Delivery times depend on destination and selected shipping method. Final shipping options and estimated delivery windows appear at checkout."),
        FaqItem("Do you offer complimentary shipping?",
            "Yes — complimentary shipping on all orders, no minimum required."),
        FaqItem("Do you ship internationally?",
            "Yes — we ship worldwide with complimentary standard shipping. Any duties or import taxes, where applicable, are calculated at checkout or collected by the carrier."),
        FaqItem("Will I receive tracking?",
            "Yes. Once your order ships, tracking details are sent to the email or phone number used at checkout. Tracking may take up to 24 hours to update after the carrier receives the package."),
        FaqItem("Can I change or cancel my order?",
            "Contact support@melato.ca immediately. We'll do our best to help before fulfillment — once an order has been processed or shipped, changes may no longer be possible."),
        FaqItem("How does Melato fit?",
            "Melato pieces are designed with a relaxed athletic fit. Choose your usual size for a clean silhouette, or size up if you prefer a looser look."),
        FaqItem("What materials are used?",
            "Materials vary by product and collection. Each product page includes its own fabric composition and care notes where available."),
        FaqItem("How do I care for my Melato pieces?",
            "Wash cold on a gentle cycle, wash inside out, avoid bleach, and hang dry when possible. Do not iron directly over embroidery, prints, appliqués, or branded details."),
        FaqItem("Will sold-out colorways restock?",
            "Melato drops are limited. Some pieces may restock, but many colorways are released in small quantities and may not return once sold out."),
        FaqItem("What is your return window?",
            "Eligible items may be returned within 30 days of delivery. Items must be unworn, unused, unwashed, with original tags attached, and in original packaging."),
        FaqItem("How do I start a return?",
            "Email support@melato.ca with your order number, item details, and return reason. Our team will review and provide next steps."),
        FaqItem("Do you offer exchanges?",
            "Exchanges depend on availability. If the item you want is limited or close to selling out, the fastest option is usually to place a new order and return the original eligible item."),
        FaqItem("What if my item arrives damaged or incorrect?",
            "Contact support@melato.ca immediately with your order number and photos. We'll assess and resolve quickly.")
    )

    fun getProductById(id: String) = products.find { it.id == id }
    fun getByCategory(cat: String) = if (cat == "all") products else products.filter { it.category == cat }
    fun getFeatured() = products.filter { it.isFeatured }
    fun getNew() = products.filter { it.isNew }
    fun search(q: String): List<Product> {
        val query = q.lowercase()
        return products.filter {
            it.title.lowercase().contains(query) ||
            it.category.lowercase().contains(query) ||
            it.description.lowercase().contains(query)
        }
    }
}
