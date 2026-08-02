package com.pedroeu.ficha.ui.sheet

import com.pedroeu.ficha.ui.i18n.trf
import com.pedroeu.ficha.ui.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedroeu.ficha.data.content.ReplicaData
import com.pedroeu.ficha.domain.ArtificerItems
import com.pedroeu.ficha.domain.KnownPlan
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.ExpandableOption
import com.pedroeu.ficha.ui.components.SectionHeader
import com.pedroeu.ficha.ui.components.SelectableCard

/**
 * Which of the Artificer's plans are made today.
 *
 * The plans themselves are a permanent list; what changes daily is which few of them exist as
 * actual objects, and that number is what the class table limits. Making one here puts it in
 * the inventory, so it turns up wherever the item would — a made weapon on the Attacks list, a
 * made shield in the Armor Class — and setting it aside takes it straight back out.
 */
@Composable
fun ArtificerItemsCard(character: PlayerCharacter, viewModel: SheetViewModel) {
    val plans = ArtificerItems.knownPlans(character)
    val made = ArtificerItems.madeItems(character)
    val allowance = ArtificerItems.allowance(character)
    val remaining = ArtificerItems.remaining(character)

    // The plan whose base item the player is choosing, if the picker is open.
    var choosingBaseFor by remember { mutableStateOf<KnownPlan?>(null) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(tr("Replicate Magic Item"))

            Text(
                text = trf("{0} of {1} made", made.size, allowance) +
                    if (remaining > 0) trf(" — room for {0} more.", remaining) else ".",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (remaining > 0) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.primary,
            )
            Text(
                text = tr("You make these when you finish a Long Rest, with Tinker's Tools in " +
                    "hand. Anything you make goes into your inventory, and a weapon or armor " +
                    "made this way reaches your attacks and Armor Class on its own."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (plans.isEmpty()) {
                Text(
                    text = tr("No plans learned yet. They're chosen with the Replicate Magic " +
                        "Item feature at levels 2, 6, 10, 14, and 18."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            plans.forEach { known ->
                val madeFromThis = made.filter { it.planId == known.plan.id }
                PlanRow(
                    known = known,
                    madeNames = madeFromThis.map { it.name },
                    canMakeMore = remaining > 0,
                    onMake = {
                        if (known.needsBase) {
                            choosingBaseFor = known
                        } else {
                            viewModel.makeArtificerItem(known.plan.id)
                        }
                    },
                    onSetAside = { name ->
                        val target = madeFromThis.first { it.name == name }
                        viewModel.unmakeArtificerItem(target.planId, target.baseId)
                    },
                )
            }
        }
    }

    choosingBaseFor?.let { known ->
        val base = known.base ?: return@let
        ReplicaBasePickerSheet(
            planName = known.plan.name,
            base = base,
            onDismiss = { choosingBaseFor = null },
            onPick = { optionId ->
                viewModel.makeArtificerItem(known.plan.id, optionId)
                choosingBaseFor = null
            },
        )
    }
}

/** One plan: what it is, what it has produced, and the button that changes that. */
@Composable
private fun PlanRow(
    known: KnownPlan,
    madeNames: List<String>,
    canMakeMore: Boolean,
    onMake: () -> Unit,
    onSetAside: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ExpandableOption(
            name = known.plan.name,
            description = known.plan.description,
            subtitle = known.plan.subtitle,
        )

        madeNames.forEach { name ->
            Row(
                Modifier.fillMaxWidth().padding(start = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = trf("Made: {0}", name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onSetAside(name) }) { Text(tr("Set aside")) }
            }
        }

        // An open-ended plan is worth offering again — it can produce a different item each
        // time it is learned — but the day's allowance still has the last word.
        Row(Modifier.fillMaxWidth().padding(start = 10.dp)) {
            TextButton(onClick = onMake, enabled = canMakeMore) {
                Text(
                    when {
                        !canMakeMore -> tr("No room today")
                        known.needsBase -> tr("Make — choose an item")
                        madeNames.isEmpty() -> tr("Make")
                        else -> tr("Make another")
                    }
                )
            }
        }
    }
}

/**
 * The filtered list a generic plan is answered from.
 *
 * The options come from the plan's own type line, so a plan that says "any Ammunition weapon"
 * shows crossbows and firearms and nothing else. Showing the full catalog and trusting the
 * player to know which entries qualify would put the rule somewhere the app can't check it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplicaBasePickerSheet(
    planName: String,
    base: ReplicaData.BaseChoice,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    val results = remember(query, base) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) base.options
        else base.options.filter {
            it.name.lowercase().contains(q) || it.supporting.lowercase().contains(q)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader(planName)
            Text(
                text = base.prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(tr("Search")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 420.dp),
            ) {
                items(results.size, key = { results[it].id }) { index ->
                    val option = results[index]
                    SelectableCard(
                        title = option.name,
                        subtitle = option.supporting,
                        selected = false,
                        onClick = { onPick(option.id) },
                    )
                }
            }
        }
    }
}
