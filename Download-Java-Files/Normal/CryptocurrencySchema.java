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

package com.exonum.binding.cryptocurrency;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.common.crypto.PublicKey;
import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.service.Schema;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.ListIndexProxy;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * A schema of the cryptocurrency service.
 *
 * <p>Has one collection: Wallets (names and values) (Merkelized)
 */
public final class CryptocurrencySchema implements Schema {

  /** A namespace of cryptocurrency service collections. */
  private static final String NAMESPACE = CryptocurrencyService.NAME.replace('-', '_');

  private final View view;

  public CryptocurrencySchema(View view) {
    this.view = checkNotNull(view);
  }

  @Override
  public List<HashCode> getStateHashes() {
    return ImmutableList.of(wallets().getRootHash());
  }

  /**
   * Returns a proof map of wallets.
   */
  public ProofMapIndexProxy<PublicKey, Wallet> wallets() {
    String name = fullIndexName("wallets");
    return ProofMapIndexProxy.newInstance(name, view, StandardSerializers.publicKey(),
        WalletSerializer.INSTANCE);
  }

  /**
   * Returns transactions history of the wallet. It contains hashes of
   * {@link com.exonum.binding.cryptocurrency.transactions.TransferTx} transaction messages
   * that changed the balance of the given wallet.
   *
   * @param walletId wallet address
   */
  public ListIndexProxy<HashCode> transactionsHistory(PublicKey walletId) {
    String name = fullIndexName("transactions_history");

    return ListIndexProxy.newInGroupUnsafe(name, walletId.toBytes(), view,
        StandardSerializers.hash());
  }

  private static String fullIndexName(String name) {
    return NAMESPACE + "__" + name;
  }
}
