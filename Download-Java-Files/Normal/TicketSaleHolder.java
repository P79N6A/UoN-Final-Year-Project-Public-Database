package io.stormbird.wallet.ui.widget.holder;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.widget.OnTokenCheckListener;
import io.stormbird.token.entity.TicketRange;

/**
 * Created by James on 12/02/2018.
 */

public class TicketSaleHolder extends BaseTicketHolder
{
    public static final int VIEW_TYPE = 1071;

    private OnTokenCheckListener onTokenCheckListener;

    private final AppCompatRadioButton select;
    private final LinearLayout ticketLayout;

    public TicketSaleHolder(int resId, ViewGroup parent, Token token, AssetDefinitionService assetService)
    {
        super(resId, parent, token, assetService);
        ticketLayout = findViewById(R.id.layout_select_ticket);
        select = findViewById(R.id.radioBox);
        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        super.bind(data, addition);
        select.setVisibility(View.VISIBLE);

        select.setOnCheckedChangeListener(null); //have to invalidate listener first otherwise we trigger cached listener and create infinite loop
        select.setChecked(data.isChecked);

        select.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                if (b)
                {
                    onTokenCheckListener.onTokenCheck(data);
                }
            }
        });

        ticketLayout.setOnClickListener(v -> {
            select.setChecked(true);
        });
    }

    public void setOnTokenCheckListener(OnTokenCheckListener onTokenCheckListener)
    {
        this.onTokenCheckListener = onTokenCheckListener;
    }
}
