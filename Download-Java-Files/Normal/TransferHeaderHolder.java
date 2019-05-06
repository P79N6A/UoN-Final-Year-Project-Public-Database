package io.stormbird.wallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;

/**
 * Created by James on 13/02/2018.
 */

public class TransferHeaderHolder extends BinderViewHolder<Token> {

    public static final int VIEW_TYPE = 1128;

    private final TextView title;

    public TransferHeaderHolder(int resId, ViewGroup parent) {
        super(resId, parent);
        title = findViewById(R.id.name);
    }

    @Override
    public void bind(@Nullable Token token, @NonNull Bundle addition)
    {
        title.setText(R.string.select_tickets_transfer);
    }
}
