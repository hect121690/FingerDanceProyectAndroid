package mega.privacy.android.app.presentation.fileinfo.view.sharedinfo

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.view.contactItemForPreviews
import mega.privacy.android.app.presentation.fileinfo.model.ContactPermission
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_SHARES_HEADER
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_SHOW_MORE
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.domain.entity.shares.AccessPermission

/**
 * Expandable list of shared contacts
 */
@Composable
internal fun SharedInfoView(
    contacts: List<ContactPermission>,
    expanded: Boolean,
    onHeaderClick: () -> Unit,
    onContactClick: (ContactPermission) -> Unit,
    onContactLongClick: (ContactPermission) -> Unit,
    onMoreOptionsClick: (ContactPermission) -> Unit,
    onShowMoreContactsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Header(contacts, expanded, onHeaderClick)
        Column(modifier = modifier.animateContentSize()) {
            if (expanded) {
                ContactsList(
                    contacts,
                    onContactClick,
                    onContactLongClick,
                    onMoreOptionsClick,
                    onShowMoreContactsClick
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun Header(
    contacts: List<ContactPermission>,
    expanded: Boolean,
    onHeaderClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = onHeaderClick)
            .height(56.dp)
            .padding(start = 72.dp)
            .fillMaxWidth()
            .testTag(TEST_TAG_SHARES_HEADER),
    ) {
        Text(
            text = stringResource(id = R.string.file_properties_shared_folder_select_contact),
            style = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.textColorPrimary)
        )
        Text(
            text =
            if (expanded) {
                stringResource(id = R.string.general_close)
            } else {
                pluralStringResource(
                    id = R.plurals.general_selection_num_contacts,
                    count = contacts.size,
                    contacts.size
                )
            },
            modifier = Modifier.padding(end = 24.dp, start = 8.dp),
            style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.secondary),
        )
    }
}

@Composable
private fun ColumnScope.ContactsList(
    contacts: List<ContactPermission>,
    onContactClick: (ContactPermission) -> Unit,
    onContactLongClick: (ContactPermission) -> Unit,
    onMoreOptionsClick: (ContactPermission) -> Unit,
    onShowMoreContactsClick: () -> Unit,
) {
    //maximum 5, so no LazyColumn needed
    contacts.take(MAX_CONTACTS_TO_SHOW).forEachIndexed { i, contactItem ->
        SharedInfoContactItemView(
            contactItem = contactItem,
            onClick = { onContactClick(contactItem) },
            onMoreOptionsClick = { onMoreOptionsClick(contactItem) },
            onLongClick = { onContactLongClick(contactItem) },
        )
        if (i < contacts.size - 1) {
            Divider(
                modifier = Modifier.padding(start = 72.dp),
                color = MaterialTheme.colors.grey_white_alpha_012,
                thickness = 1.dp
            )
        }
    }
    (contacts.size - MAX_CONTACTS_TO_SHOW).takeIf { it > 0 }?.let { extra ->
        Text(
            text = "$extra ${stringResource(id = R.string.label_more)}",
            modifier = Modifier
                .padding(2.dp)
                .clickable(onClick = onShowMoreContactsClick)
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .align(Alignment.End)
                .testTag(TEST_TAG_SHOW_MORE),
            style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.secondary),
        )
    }
}

internal const val MAX_CONTACTS_TO_SHOW = 5

/**
 * Preview for [SharedInfoView]
 */
@CombinedThemePreviews
@Composable
private fun SharedInfoPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        var expanded by remember { mutableStateOf(true) }
        SharedInfoView(
            contacts = List(7) {
                ContactPermission(
                    contactItemForPreviews,
                    AccessPermission.values()[it.mod(AccessPermission.values().size)]
                )
            },
            expanded = expanded,
            onHeaderClick = { expanded = !expanded },
            onContactClick = {},
            onContactLongClick = {},
            onMoreOptionsClick = {},
            onShowMoreContactsClick = {},
        )
    }
}