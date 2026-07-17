(ns culture.facts
  "Country-level regional-culture catalog for Trinidad and Tobago (TTO) --
  national dishes, protected products, beverages, crafts, festivals and
  heritage sites, per ADR-2607171400 addendum 2 (cloud-itonami-
  municipality-culture-catalog Wave 1, in com-junkawasaki/root). Sibling
  namespace to `marketentry.facts` / `statute.facts` (ADR-2607141700);
  city-level counterparts live in the cloud-itonami-municipality-* repos.

  Catalog is keyed by UPPERCASE ISO3 (mirrors `statute.facts`); entries
  carry no :culture/municipality (that attribute is city-level only).

  Every entry cites a source URL that was actually fetched and read on
  :culture/retrieved-at -- never fabricated. Summaries state only what the
  cited source confirms. An item not in this table has NO spec-basis, full
  stop; extend `catalog`, do not invent an id/url.")

(def catalog
  "iso3 -> vector of culture entries."
  {"TTO"
   [{:culture/id "tto.dish.doubles"
     :culture/name "Doubles"
     :culture/country "TTO"
     :culture/kind :dish
     :culture/summary "Caribbean street food of curried chickpeas served on two fried flatbreads, part of Indo-Trinidadian cuisine and credited to Emamool Deen in 1936."
     :culture/url "https://en.wikipedia.org/wiki/Doubles_(food)"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "tto.dish.pelau"
     :culture/name "Pelau"
     :culture/country "TTO"
     :culture/kind :dish
     :culture/summary "Traditional West Indies rice dish that originated in Trinidad when Indian indentured servants shared pilaf preparation methods with African slaves, who adapted it with caramelization techniques and local Caribbean ingredients."
     :culture/url "https://en.wikipedia.org/wiki/Pelau"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "tto.dish.callaloo"
     :culture/name "Callaloo"
     :culture/country "TTO"
     :culture/kind :dish
     :culture/summary "One of the national dishes of Trinidad and Tobago (shared with Dominica); Trinidadians prepare it with taro/dasheen bush and okra with coconut milk, distinct from Jamaica's amaranth-based version."
     :culture/url "https://en.wikipedia.org/wiki/Callaloo"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "tto.dish.bake-and-shark"
     :culture/name "Bake and shark"
     :culture/country "TTO"
     :culture/kind :dish
     :culture/summary "Traditional fast food dish of Trinidadian cuisine, prepared with fried flatbread, shark meat, and additional ingredients; particularly associated with Maracas Beach."
     :culture/url "https://en.wikipedia.org/wiki/Bake_and_shark"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "tto.product.angostura-bitters"
     :culture/name "Angostura bitters"
     :culture/country "TTO"
     :culture/kind :product
     :culture/summary "Concentrated bitters based on gentian, herbs, and spices, produced by the House of Angostura; though originally developed in Venezuela, it has been manufactured in Trinidad since 1875 and remains a significant national export."
     :culture/url "https://en.wikipedia.org/wiki/Angostura_bitters"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "tto.craft.steelpan"
     :culture/name "Steelpan"
     :culture/country "TTO"
     :culture/kind :craft
     :culture/summary "Percussion instrument made from steel drums, originating in Trinidad and Tobago and declared the country's national instrument in 1992."
     :culture/url "https://en.wikipedia.org/wiki/Steelpan"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "tto.festival.carnival"
     :culture/name "Trinidad and Tobago Carnival"
     :culture/country "TTO"
     :culture/kind :festival
     :culture/summary "Annual event held on the Monday and Tuesday before Ash Wednesday, featuring colorful costumes, calypso and soca music, stick-fighting, limbo, and steelpan competitions, with roots dating to the 1780s."
     :culture/url "https://en.wikipedia.org/wiki/Trinidad_and_Tobago_Carnival"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}
    {:culture/id "tto.heritage.pitch-lake"
     :culture/name "Pitch Lake"
     :culture/country "TTO"
     :culture/kind :heritage
     :culture/summary "Largest natural deposit of bitumen in the world, estimated to contain 10 million tons, situated in southwest Trinidad and a significant natural resource and tourist destination."
     :culture/url "https://en.wikipedia.org/wiki/Pitch_Lake"
     :culture/url-provenance :wikipedia-en
     :culture/retrieved-at "2026-07-17"}]})

(defn spec-basis [iso3] (get catalog iso3))

(defn coverage
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-iso3166-tto culture catalog "
                 "(ADR-2607171400 addendum 2, Wave 1): " (count (get catalog "TTO"))
                 " TTO entries, each with a fetched-and-read citation. "
                 "Extend `culture.facts/catalog`, never fabricate an id/url.")})))

(defn by-kind [iso3 kind]
  (filterv #(= (:culture/kind %) kind) (spec-basis iso3)))
