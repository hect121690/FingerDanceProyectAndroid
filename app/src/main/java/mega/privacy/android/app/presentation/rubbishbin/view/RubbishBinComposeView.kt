package mega.privacy.android.app.presentation.rubbishbin.view

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.rubbishbin.model.RubbishBinState
import mega.privacy.android.app.presentation.view.NODES_EMPTY_VIEW_VISIBLE
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.core.ui.controls.MegaEmptyView
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * View for RubbishBinComposeFragment
 * @param uiState [RubbishBinState]
 * @param stringUtilWrapper [StringUtilWrapper]
 * @param onMenuClick
 * @param onItemClicked
 * @param onLongClick
 * @param onSortOrderClick
 * @param onChangeViewTypeClick
 * @param sortOrder
 * @param emptyState
 */
@Composable
fun RubbishBinComposeView(
    uiState: RubbishBinState,
    stringUtilWrapper: StringUtilWrapper,
    onMenuClick: (NodeUIItem) -> Unit,
    onItemClicked: (NodeUIItem) -> Unit,
    onLongClick: (NodeUIItem) -> Unit,
    onSortOrderClick: () -> Unit,
    onChangeViewTypeClick: () -> Unit,
    sortOrder: String,
    emptyState: Pair<Int, Int>
) {
    if (uiState.nodeList.isNotEmpty()) {
        NodesView(
            modifier = Modifier
                .padding(horizontal = 8.dp),
            nodeUIItems = uiState.nodeList,
            stringUtilWrapper = stringUtilWrapper,
            onMenuClick = onMenuClick,
            onItemClicked = onItemClicked,
            onLongClick = onLongClick,
            sortOrder = sortOrder,
            isListView = uiState.currentViewType == ViewType.LIST,
            onSortOrderClick = onSortOrderClick,
            onChangeViewTypeClick = onChangeViewTypeClick,
        )
    } else {
        MegaEmptyView(
            modifier = Modifier.testTag(NODES_EMPTY_VIEW_VISIBLE),
            imagePainter = painterResource(id = emptyState.first),
            text = stringResource(id = emptyState.second)
        )
    }
}