package org.xtext.example.mydsl.ide

import com.google.inject.Inject
import com.google.inject.Provider
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionKind
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.WorkspaceEdit
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.xtext.ide.refactoring.IRenameStrategy2
import org.eclipse.xtext.ide.refactoring.RenameChange
import org.eclipse.xtext.ide.refactoring.RenameContext
import org.eclipse.xtext.ide.serializer.IChangeSerializer
import org.eclipse.xtext.ide.server.codeActions.ICodeActionService2
import org.eclipse.xtext.ide.server.rename.ChangeConverter2
import org.eclipse.xtext.ide.server.rename.ServerRefactoringIssueAcceptor
import org.eclipse.xtext.resource.EObjectAtOffsetHelper
import org.eclipse.xtext.resource.XtextResource
import org.xtext.example.mydsl.validation.MyDslValidator
import org.xtext.example.mydsl.myDsl.Greeting

class MyDslCodeActionService implements ICodeActionService2 {
	
	@Inject extension EObjectAtOffsetHelper

	@Inject IRenameStrategy2 renameStrategy

	@Inject ChangeConverter2.Factory converterFactory

	@Inject Provider<IChangeSerializer> changeSerializerProvider

	@Inject Provider<ServerRefactoringIssueAcceptor> issueProvider

	protected def addTextEdit(WorkspaceEdit edit, URI uri, TextEdit... textEdit) {
		edit.changes.put(uri.toString, textEdit)
	}

	override getCodeActions(Options options) {
		val document = options.document
		val params = options.codeActionParams
		val resource = options.resource
		val result = <CodeAction>newArrayList
		for (d : params.context.diagnostics) {
			if (d.code == MyDslValidator.INVALID_NAME) {
				val text = document.getSubstring(d.range)
				result += new CodeAction => [
					kind = CodeActionKind.QuickFix
					title = "Capitalize Name"
					diagnostics = #[d]
					
					val offset = document.getOffSet(d.range.start)
					val element = (resource as XtextResource).resolveElementAt(offset)
					val workspaceEdit = new WorkspaceEdit
//					val change = new RenameChange(text.toFirstUpper, EcoreUtil.getURI(element))
					val changeSerializer = changeSerializerProvider.get
//					val issueAcceptor = issueProvider.get
//					val resourceSet = options.languageServerAccess.newLiveScopeResourceSet(options.resource.URI)
//					val context = new RenameContext(#[change], resourceSet, changeSerializer, issueAcceptor)
					changeSerializer.addModification(element)[
						e|
						val g = (e as Greeting)
						g.name = g.name.toFirstUpper
					]
					//renameStrategy.applyRename(context)
					val changeConverter = converterFactory.create(workspaceEdit, options.languageServerAccess)
					changeSerializer.applyModifications(changeConverter)
					edit = workspaceEdit
					
					// for more complex example we would use 
					// change serializer as in RenameService
//					edit = new WorkspaceEdit() => [
//						addTextEdit(resource.URI, new TextEdit => [
//							range = d.range
//							newText = text.toFirstUpper
//						])
//					]

				]

			}
		}
		return result.map[Either.forRight(it)]
	}

}
