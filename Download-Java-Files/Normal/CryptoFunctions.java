/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.common.crypto;

import com.goterl.lazycode.lazysodium.interfaces.Sign;

/**
 * A collection of public-key signature system crypto functions.
 */
public final class CryptoFunctions {

  private CryptoFunctions() {}

  /**
   * Returns a ED25519 public-key signature system crypto function.
   */
  public static CryptoFunction ed25519() {
    return Ed25519CryptoFunction.INSTANCE;
  }

  public static class Ed25519 {
    public static final int SEED_BYTES = Sign.ED25519_SEEDBYTES;
    public static final int SIGNATURE_BYTES = Sign.ED25519_BYTES;
    public static final int PRIVATE_KEY_BYTES = Sign.SECRETKEYBYTES;
    public static final int PUBLIC_KEY_BYTES = Sign.PUBLICKEYBYTES;
  }

}
