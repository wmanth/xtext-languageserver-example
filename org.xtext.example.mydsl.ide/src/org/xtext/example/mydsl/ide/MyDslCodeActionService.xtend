package org.xtext.example.mydsl.ide

import com.google.inject.Inject
import com.google.inject.Provider
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionKind
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.WorkspaceEdit
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.xtext.ide.serializer.IChangeSerializer
import org.eclipse.xtext.ide.serializer.IEmfResourceChange
import org.eclipse.xtext.ide.serializer.ITextDocumentChange
import org.eclipse.xtext.ide.server.codeActions.ICodeActionService2
import org.eclipse.xtext.resource.EObjectAtOffsetHelper
import org.eclipse.xtext.resource.XtextResource
import org.eclipse.xtext.util.CollectionBasedAcceptor
import org.xtext.example.mydsl.myDsl.Greeting
import org.xtext.example.mydsl.validation.MyDslValidator

class MyDslCodeActionService implements ICodeActionService2 {
	
	@Inject extension EObjectAtOffsetHelper

	@Inject Provider<IChangeSerializer> changeSerializerProvider

	protected def addTextEdit(WorkspaceEdit edit, URI uri, TextEdit... textEdit) {
		edit.changes.put(uri.toString, textEdit)
	}

	override getCodeActions(Options options) {
		val params = options.codeActionParams
		val result = <CodeAction>newArrayList
		for (d : params.context.diagnostics) {
			if (d.code == MyDslValidator.INVALID_NAME) {
				val ca = capitalizeName(d, options)
				if (ca.edit !== null && (!ca.edit.documentChanges.isNullOrEmpty || !(ca.edit.changes === null || ca.edit.changes.empty))) {
					result.add(ca)
				}
			}
		}
		return result.map[Either.forRight(it)]
	}
	
	def private CodeAction capitalizeName(Diagnostic d, Options options) {
		val wsEdit = recordWorkspaceEdit(options) [ copiedResource |
			val offset =options.document.getOffSet(d.range.start)
			val element = (copiedResource as XtextResource).resolveElementAt(offset)
			if (element instanceof Greeting) {
				element.name = element.name.toFirstUpper
			}
		]
		return new CodeAction => [
			kind = CodeActionKind.QuickFix
			title = "Capitalize Name"
			diagnostics = #[d]
			edit = wsEdit
		]
	}
	
	
	def private WorkspaceEdit recordWorkspaceEdit(Options options, IChangeSerializer.IModification<Resource> mod) {
		val serializer = changeSerializerProvider.get
		val rs = options.languageServerAccess.newLiveScopeResourceSet(options.resource.URI)
		val copy = rs.getResource(options.resource.URI, true)
		serializer.addModification(copy, mod)
		val documentchanges = <IEmfResourceChange>newArrayList()
		serializer.applyModifications(CollectionBasedAcceptor.of(documentchanges))
		return new WorkspaceEdit => [
			for (documentchange : documentchanges.filter(ITextDocumentChange)) {
				val edits = documentchange.replacements.map [ replacement |
					new TextEdit => [
						newText = replacement.replacementText
						range = new Range(options.document.getPosition(replacement.offset), options.document.getPosition(replacement.endOffset))
					]
				]
				changes.put(documentchange.newURI.toString, edits)
			}
		]
	}

}
